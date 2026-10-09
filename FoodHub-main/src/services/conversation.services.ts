import { ConversationModel, ConversationStatus, IConversation } from "~/models/mongodb/conversation.model"
import { MessageModel } from "~/models/mongodb/message.model"
import { prisma } from '~/config/prisma'

export class ConversationServices {
  static async listForHosts() {
    const conversations = await ConversationModel.find({ status: ConversationStatus.OPEN })
      .sort({ lastMessageAt: -1, updatedAt: -1 })
      .lean()
    const customerIds = conversations.map((item) => item.customerId)
    const [users, tables] = await Promise.all([
      prisma.user.findMany({ where: { id: { in: customerIds } }, select: { id: true, name: true } }),
      prisma.table.findMany({ where: { id: { in: customerIds } }, select: { id: true, name: true, floor: true } })
    ])
    const userNames = new Map(users.map((user) => [user.id, user.name]))
    const tableNames = new Map(tables.map((table) => [
      table.id,
      [table.name, table.floor].filter(Boolean).join(' • ')
    ]))
    return conversations.map((item) => ({
      ...item,
      customerName: tableNames.get(item.customerId) || userNames.get(item.customerId) || 'Khách hàng'
    }))
  }

  static async close(conversationId: string) {
    return ConversationModel.findByIdAndUpdate(
      conversationId,
      { status: ConversationStatus.CLOSED },
      { new: true }
    ).lean()
  }

  static async closeForCustomer(customerId: string) {
    return ConversationModel.updateMany(
      { customerId, status: ConversationStatus.OPEN },
      { status: ConversationStatus.CLOSED }
    )
  }
  static async getOrCreateForCustomer(customerId: string): Promise<{ conversation: IConversation, isNew: boolean }> {
    let conversation = await ConversationModel.findOne({
      customerId,
      status: ConversationStatus.OPEN
    })
    if (conversation) {
      return { conversation, isNew: false }
    }

    conversation = await ConversationModel.create({
      customerId,
      status: ConversationStatus.OPEN,
      assignedHostId: null,
      assignedAt: null,
      lastMessage: null,
      lastMessageSenderId: null,
      lastMessageAt: null
    })

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
