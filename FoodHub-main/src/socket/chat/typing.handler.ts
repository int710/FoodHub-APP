import { Server, Socket } from "socket.io"
import { getConversationRoom } from "../socket.room"
import { ConversationModel } from "~/models/mongodb/conversation.model"
import { ConversationServices } from '~/services/conversation.services'
import { isSocketSessionActive } from '../socket.middleware'

interface TypingPayload {
  conversationId: string
}

const canAccessConversation = async (socket: Socket, conversationId: string) => {
  const user = socket.data.user
  if (!user || !(await isSocketSessionActive(socket))) return false

  const ownerId = user.authType === 'TABLE_GUEST' ? user.sessionId : user.user_id
  const conversation = await ConversationModel.findById(conversationId)
  if (!conversation || !(await ConversationServices.isActive(conversation))) return false
  return user.role === 'STAFF' || user.role === 'ADMIN' || conversation.customerId === ownerId
}

export const RegisterTypingSocket = (_io: Server, socket: Socket): void => {
  socket.on('typing:start', async (payload: TypingPayload) => {
    const user = socket.data.user
    const { conversationId } = payload || {}
    if (!conversationId || !user || !(await canAccessConversation(socket, conversationId))) return
    const room = getConversationRoom(conversationId)
    socket.to(room).emit('typing:start', {
      conversationId,
      userId: String(user.user_id),
      role: user.role
    })
  })

  socket.on('typing:stop', async (payload: TypingPayload) => {
    const user = socket.data.user
    const { conversationId } = payload || {}
    if (!conversationId || !user || !(await canAccessConversation(socket, conversationId))) return

    const room = getConversationRoom(conversationId)
    socket.to(room).emit('typing:stop', {
      conversationId,
      userId: String(user.user_id)
    })
  })
}
