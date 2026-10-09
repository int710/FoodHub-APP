import { ConversationModel, ConversationStatus, IConversation } from "~/models/mongodb/conversation.model"
import { MessageModel } from "~/models/mongodb/message.model"
import { prisma } from '~/config/prisma'
import { redis } from '~/config/redis'
import { RedisKey } from '~/constants/redis'
import { getOptionalSocketIO } from '~/socket/socket.instance'
import { getConversationRoom, HOST_ROOM } from '~/socket/socket.room'

export class ConversationServices {
  static async listForHosts() {
    const conversations = await ConversationModel.find({ status: ConversationStatus.OPEN })
      .sort({ lastMessageAt: -1, updatedAt: -1 })
      .lean()
    const active = []
    for (const conversation of conversations) {
      if (await this.isActive(conversation)) active.push(conversation)
    }
    const customerIds = active.map((item) => item.tableId || item.customerId)
    const [users, tables] = await Promise.all([
      prisma.user.findMany({ where: { id: { in: customerIds } }, select: { id: true, name: true } }),
      prisma.table.findMany({ where: { id: { in: customerIds } }, select: { id: true, name: true, floor: true } })
    ])
    const userNames = new Map(users.map((user) => [user.id, user.name]))
    const tableNames = new Map(tables.map((table) => [
      table.id,
      [table.name, table.floor].filter(Boolean).join(' • ')
    ]))
    const visible = []
    for (const item of active) {
      if (!item.sessionId && tableNames.has(item.customerId)) await this.close(String(item._id))
      else visible.push(item)
    }
    return visible.map((item) => ({
      ...item,
      customerName: item.sessionId
        ? `${tableNames.get(item.tableId!) || 'Bàn'} • Phiên ${item.sessionId.slice(0, 8)}`
        : userNames.get(item.customerId) || 'Khách hàng'
    }))
  }

  static async close(conversationId: string) {
    const conversation = await ConversationModel.findByIdAndUpdate(
      conversationId,
      { status: ConversationStatus.CLOSED },
      { new: true }
    ).lean()
    if (conversation) {
      const io = getOptionalSocketIO()
      const room = getConversationRoom(String(conversation._id))
      io?.to(room).emit('conversation:closed', { conversationId: String(conversation._id) })
      io?.to(HOST_ROOM).emit('conversation:updated', conversation)
      io?.in(room).socketsLeave(room)
    }
    return conversation
  }

  static async closeForCustomer(customerId: string) {
    const conversations = await ConversationModel.find({ customerId, status: ConversationStatus.OPEN }).select('_id')
    await Promise.all(conversations.map((item) => this.close(String(item._id))))
  }
  static async isActive(conversation: IConversation & { _id?: unknown }): Promise<boolean> {
    if (conversation.status !== ConversationStatus.OPEN) return false
    if (conversation.tableId && conversation.sessionId) {
      const tableId = await redis.get(RedisKey.tableSession(conversation.sessionId))
      if (tableId !== conversation.tableId || (conversation.expiresAt && conversation.expiresAt.getTime() <= Date.now())) {
        await this.close(String(conversation._id))
        return false
      }
    }
    return true
  }

  static async getOrCreateForCustomer(customerId: string, tableSession?: { tableId: string; sessionId: string; expiresAt: Date }): Promise<{ conversation: IConversation, isNew: boolean }> {
    let conversation = await ConversationModel.findOne({
      customerId,
      status: ConversationStatus.OPEN
    })
    if (conversation) {
      return { conversation, isNew: false }
    }

    try {
      conversation = await ConversationModel.create({
        customerId,
        ...tableSession,
        status: ConversationStatus.OPEN,
        assignedHostId: null,
        assignedAt: null,
        lastMessage: null,
        lastMessageSenderId: null,
        lastMessageAt: null
      })
    } catch (error) {
      if ((error as { code?: number }).code !== 11000) throw error
      conversation = await ConversationModel.findOne({ customerId, status: ConversationStatus.OPEN })
      if (!conversation) throw error
      return { conversation, isNew: false }
    }

    return { conversation, isNew: true }
  }

  static async updatedLastMessage(conversationId: string, content: string, senderId: string): Promise<IConversation | null> {
    return await ConversationModel.findByIdAndUpdate(conversationId,
      {
        lastMessage: content,
        lastMessageSenderId: senderId,
        lastMessageAt: new Date()
      }, {
      new: true
    })
  }

  static async assignedHostIfUnassigned(conversationId: string, hostId: string): Promise<{ conversation: IConversation | null; isAssigned: boolean }> {
    const conversation = await ConversationModel.findById(conversationId)
    if (!conversation) return { conversation: null, isAssigned: false }
    if (!conversation.assignedHostId) {
      conversation.assignedHostId = hostId
      conversation.assignedAt = new Date()
      await conversation.save()
      return { conversation, isAssigned: true }
    }
    return { conversation, isAssigned: false }
  }

  static async getMessageHistory({ conversationId, page = 1, limit = 20 }: {
    conversationId: string;
    page?: number | undefined;
    limit?: number | undefined
  }) {
    const skip = (page - 1) * limit;
    const messages = await MessageModel.find({ conversationId }).sort({ createdAt: -1 }).skip(skip).limit(limit).lean();

    const total = await MessageModel.countDocuments({ conversationId });
    const hasMore = skip + messages.length < total;

    return {
      conversationId,
      messages: messages.reverse(),
      pagination: {
        page: Number(page),
        limit: Number(limit),
        hasMore
      }
    }
  }
}
