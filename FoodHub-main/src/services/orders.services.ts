import { prisma } from "~/config/prisma"
import { ItemStatus, OrderStatus, Role } from "~/generated/prisma/enums"
import { HistoryQuery, KitchenQuery } from "~/models/schemas/order.schema"
import HTTP_STATUS from '~/constants/httpStatus'
import { ErrorWithStatus } from '~/models/Errors'
import { PaymentStatus } from '~/generated/prisma/enums'
import { emitOrderStatusUpdate } from "~/socket/orders/order.emitter"
import notificationsServices from "~/services/notifications.services"
import tableServices from '~/services/tables.services'

const orderDetailInclude = {
  table: {
    select: { id: true, name: true, floor: true }
  },
  customer: {
    select: { id: true, name: true, email: true, phone: true }
  },
  confirmedBy: {
    select: { id: true, name: true, email: true }
  },
  items: {
    orderBy: { createdAt: 'asc' as const },
    include: {
      menuItem: {
        select: { id: true, name: true, image: true }
      }
    }
  },
  payments: {
    orderBy: { createdAt: 'desc' as const },
    select: {
      id: true,
      method: true,
      status: true,
      amount: true,
      txnRef: true,
      paidAt: true,
      createdAt: true
    }
  }
}


class OrdersServices {
  async getHistory(actorId: string, actorRole: Role, query: HistoryQuery) {
    const { limit, page, customerId, from, status, tableId, to, type } = query
    const skip = (page - 1) * limit

    // customer chỉ được xem order của họ
    // staff và admin xem tất cả
    const where = {
      ...(actorRole === Role.CUSTOMER ? { customerId: actorId } : {}),
      ...(actorRole === Role.ADMIN && customerId ? { customerId } : {}),
      ...(status ? { status } : {}),
      ...(type ? { type } : {}),
      ...(tableId ? { tableId } : {}),
      ...(from || to
        ? {
          createdAt: {
            ...(from ? { gte: from } : {}),
            ...(to ? { lte: to } : {})
          }
        }
        : {})
    }

    const [orders, total] = await prisma.$transaction([
      prisma.order.findMany({
        where, skip, take: limit, orderBy: { createdAt: 'desc' }, include: orderDetailInclude
      }),
      prisma.order.count({ where })
    ])

    return {
      orders,
      pagination: { page, limit, total }
    }
  }

  async getKitchenOrders(query: KitchenQuery) {
    const page = query.page
    const limit = query.limit
    const skip = (page - 1) * limit
    const itemStatus = query.status ? [query.status] : [ItemStatus.WAITING, ItemStatus.PREPARING]

    const where = {
      status: { in: itemStatus },
      order: {
        is: {
          status: { in: [OrderStatus.CONFIRMED, OrderStatus.PREPARING] },
          OR: [
            { paidAt: { not: null } },
            { payments: { some: { status: PaymentStatus.PAID } } }
          ]
        }
      }
    }

    const [items, total] = await prisma.$transaction([
      prisma.orderItem.findMany({
        where,
        skip,
        take: limit,
        orderBy: { createdAt: 'asc' },
        select: {
          id: true,
          orderId: true,
          quantity: true,
          note: true,
          snapshot: true,
          status: true,
          createdAt: true,
          updatedAt: true,
          order: {
            select: {
              id: true,
              type: true,
              status: true,
              note: true,
              pickupCode: true,
              createdAt: true,
              table: {
                select: { id: true, name: true, floor: true }
              }
            }
          }
        }
      }),
      prisma.orderItem.count({ where })
    ])

    return {
      items,
      pagination: { page, limit, total }
    }
  }
  async confirmOrder(orderId: string, staffId: string) {
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      select: { id: true, status: true }
    })

    if (!order) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.NOT_FOUND,
        message: 'Không tìm thấy đơn hàng'
      })
    }

    if (order.status !== OrderStatus.PENDING_CONFIRMATION) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.BAD_REQUEST,
        message: 'Chỉ có thể xác nhận đơn đang chờ'
      })
    }

    const updatedOrder = await prisma.$transaction(async (tx) => {
      const result = await tx.order.updateMany({
        where: { id: orderId, status: OrderStatus.PENDING_CONFIRMATION },
        data: { status: OrderStatus.CONFIRMED, confirmedById: staffId, confirmAt: new Date() }
      })
      if (result.count === 0) {
        throw new ErrorWithStatus({ httpStatusCode: HTTP_STATUS.CONFLICT, message: 'Đơn đã thay đổi trạng thái. Vui lòng tải lại đơn.' })
      }
      return tx.order.findUniqueOrThrow({ where: { id: orderId }, include: orderDetailInclude })
    })

    emitOrderStatusUpdate({
      orderId: updatedOrder.id,
      orderType: updatedOrder.type,
      previousStatus: OrderStatus.PENDING_CONFIRMATION,
      status: updatedOrder.status,
      updatedAt: (updatedOrder.confirmAt || new Date()).toISOString(),
      updatedBy: {
        userId: staffId,
        role: 'STAFF'
      }
    })

    await notificationsServices.createOrderStatusNotification({
      orderId: updatedOrder.id,
      previousStatus: OrderStatus.PENDING_CONFIRMATION,
      status: updatedOrder.status,
      actorRole: Role.STAFF
    }).catch((error) => {
      console.error('[Notification] Failed to create confirmation notification:', error)
    })

    return updatedOrder
  }

  async rejectOrder(orderId: string, reason: string, staffId?: string, role?: 'STAFF' | 'ADMIN'
  ) {
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      include: {
        payments: {
          select: { status: true }
        }
      }
    })

    if (!order) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.NOT_FOUND,
        message: 'Không tìm thấy đơn hàng'
      })
    }

    if (order.status !== OrderStatus.PENDING_CONFIRMATION) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.BAD_REQUEST,
        message: 'Chỉ có thể từ chối đơn đang ở trạng thái chờ xác nhận'
      })
    }

    if (order.payments.some((payment) => payment.status === PaymentStatus.PAID)) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.CONFLICT,
        message: 'Đơn đã thanh toán, cần xử lý hoàn tiền trước khi từ chối'
      })
    }

    const previousStatus = order.status

    const updatedOrder = await prisma.$transaction(async (tx) => {
      const result = await tx.order.updateMany({
        where: {
          id: orderId, status: OrderStatus.PENDING_CONFIRMATION, paidAt: null,
          payments: { none: { status: PaymentStatus.PAID } }
        },
        data: { status: OrderStatus.CANCELLED, cancelReason: reason, expireAt: null }
      })
      if (result.count === 0) {
        throw new ErrorWithStatus({ httpStatusCode: HTTP_STATUS.CONFLICT, message: 'Đơn đã thanh toán hoặc thay đổi trạng thái. Vui lòng tải lại đơn.' })
      }
      return tx.order.findUniqueOrThrow({ where: { id: orderId }, include: orderDetailInclude })
    })

    emitOrderStatusUpdate({
      orderId: updatedOrder.id,
      orderType: updatedOrder.type,
      previousStatus,
      status: updatedOrder.status,
      updatedAt: updatedOrder.updatedAt ? updatedOrder.updatedAt.toISOString() : new Date().toISOString(),
      updatedBy: staffId ? {
        userId: staffId,
        role: role || 'STAFF'
      } : undefined
    })

    await notificationsServices.createOrderStatusNotification({
      orderId: updatedOrder.id,
      previousStatus,
      status: updatedOrder.status,
      actorRole: role === 'ADMIN' ? Role.ADMIN : Role.STAFF,
      reason
    }).catch((error) => {
      console.error('[Notification] Failed to create rejection notification:', error)
    })

    return updatedOrder
  }

  async updateKitchenItemStatus(itemId: string, status: ItemStatus) {
    return prisma.$transaction(async (tx) => {
      const item = await tx.orderItem.findUnique({
        where: { id: itemId },
        select: {
          id: true,
          orderId: true,
          status: true,
          order: {
            select: {
              id: true,
              type: true,
              status: true,
              payments: { select: { status: true } }
            }
          }
        }
      })

      if (!item) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.NOT_FOUND,
          message: 'Không tìm thấy món trong đơn'
        })
      }

      if (
        item.order.status !== OrderStatus.CONFIRMED &&
        item.order.status !== OrderStatus.PREPARING
      ) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.BAD_REQUEST,
          message: 'Đơn không ở trạng thái có thể chế biến'
        })
      }

      if (item.status === ItemStatus.READY) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.BAD_REQUEST,
          message: 'Món này đã hoàn thành'
        })
      }

      if (!item.order.payments.some((payment) => payment.status === PaymentStatus.PAID)) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.CONFLICT,
          message: 'Phải xác nhận thanh toán trước khi chế biến món'
        })
      }

      const previousItemStatus = item.status
      const initialOrderStatus = item.order.status

      let orderStatusChanged = false
      let updatedOrder = null

      const updatedItem = await tx.orderItem.update({
        where: { id: itemId },
        data: { status },
        include: {
          menuItem: {
            select: { id: true, name: true, image: true }
          }
        }
      })

      if (status === ItemStatus.PREPARING && initialOrderStatus === OrderStatus.CONFIRMED) {
        updatedOrder = await tx.order.update({
          where: { id: item.orderId },
          data: { status: OrderStatus.PREPARING }
        })
        orderStatusChanged = true
      }

      if (status === ItemStatus.READY) {
        const unfinishedCount = await tx.orderItem.count({
          where: {
            orderId: item.orderId,
            status: { not: ItemStatus.READY }
          }
        })

        if (unfinishedCount === 0) {
          updatedOrder = await tx.order.update({
            where: { id: item.orderId },
            data: {
              status: OrderStatus.READY,
              readyAt: new Date()
            }
          })
          orderStatusChanged = true
        }
      }

      return {
        updatedItem,
        previousItemStatus,
        orderStatusChanged,
        previousOrderStatus: initialOrderStatus,
        order: updatedOrder || item.order
      }
    })
  }

  async serveOrder(orderId: string) {
    const updatedOrder = await prisma.$transaction(async (tx) => {
      const order = await tx.order.findUnique({
        where: { id: orderId },
        select: {
          id: true,
          status: true,
          items: {
            select: { status: true }
          }
        }
      })

      if (!order) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.NOT_FOUND,
          message: 'Không tìm thấy đơn hàng'
        })
      }

      if (order.status !== OrderStatus.READY) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.BAD_REQUEST,
          message: 'Chỉ có thể phục vụ đơn đã hoàn thành'
        })
      }

      if (order.items.some((item) => item.status !== ItemStatus.READY)) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.BAD_REQUEST,
          message: 'Vẫn còn món chưa hoàn thành'
        })
      }

      await tx.orderItem.updateMany({
        where: { orderId },
        data: { status: ItemStatus.SERVED }
      })

      return tx.order.update({
        where: { id: orderId },
        data: {
          status: OrderStatus.SERVED,
          servedAt: new Date()
        },
        include: orderDetailInclude
      })
    })
    emitOrderStatusUpdate({
      orderId: updatedOrder.id,
      orderType: updatedOrder.type,
      previousStatus: OrderStatus.READY,
      status: updatedOrder.status,
      updatedAt: (updatedOrder.servedAt || new Date()).toISOString()
    })

    await notificationsServices.createOrderStatusNotification({
      orderId: updatedOrder.id,
      previousStatus: OrderStatus.READY,
      status: updatedOrder.status
    }).catch((error) => {
      console.error('[Notification] Failed to create served notification:', error)
    })

    return updatedOrder
  }

  async completeOrder(orderId: string) {
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      include: { payments: { select: { status: true } } }
    })
    if (!order) {
      throw new ErrorWithStatus({ httpStatusCode: HTTP_STATUS.NOT_FOUND, message: 'Không tìm thấy đơn hàng' })
    }
    if (order.status !== OrderStatus.SERVED) {
      throw new ErrorWithStatus({ httpStatusCode: HTTP_STATUS.BAD_REQUEST, message: 'Chỉ có thể hoàn tất đơn đã phục vụ' })
    }
    if (!order.payments.some((payment) => payment.status === PaymentStatus.PAID)) {
      throw new ErrorWithStatus({ httpStatusCode: HTTP_STATUS.CONFLICT, message: 'Đơn chưa được thanh toán' })
    }
    const updated = await prisma.order.update({
      where: { id: orderId },
      data: { status: OrderStatus.COMPLETED },
      include: orderDetailInclude
    })
    if (updated.tableId) {
      const remainingOrders = await prisma.order.count({
        where: {
          tableId: updated.tableId,
          status: { notIn: [OrderStatus.COMPLETED, OrderStatus.CANCELLED] }
        }
      })
      if (remainingOrders === 0) await tableServices.endAllSessions(updated.tableId)
    }
    emitOrderStatusUpdate({
      orderId: updated.id,
      orderType: updated.type,
      previousStatus: OrderStatus.SERVED,
      status: updated.status,
      updatedAt: updated.updatedAt.toISOString()
    })
    await notificationsServices.createOrderStatusNotification({
      orderId: updated.id,
      previousStatus: OrderStatus.SERVED,
      status: updated.status
    }).catch((error) => console.error('[Notification] Failed to create completion notification:', error))
    return updated
  }

  async cancelOrder(orderId: string, actorId: string, actorRole: Role, reason: string) {
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      include: {
        payments: {
          select: { status: true }
        }
      }
    })

    if (!order) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.NOT_FOUND,
        message: 'Không tìm thấy đơn hàng'
      })
    }

    // Customer chỉ hủy đơn của mình.
    if (actorRole === Role.CUSTOMER && order.customerId !== actorId) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.FORBIDDEN,
        message: 'Bạn không có quyền hủy đơn này'
      })
    }

    // Restore the original role-based cancellation policy.
    const allowedStatuses: OrderStatus[] = actorRole === Role.CUSTOMER
      ? [OrderStatus.PENDING_CONFIRMATION]
      : [OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED]
    const canCancel = allowedStatuses.includes(order.status)

    if (!canCancel) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.BAD_REQUEST,
        message: 'Đơn hiện không thể hủy'
      })
    }

    if (order.paidAt || order.payments.some((payment) => payment.status === PaymentStatus.PAID)) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.CONFLICT,
        message: 'Đơn đã thanh toán, cần xử lý hoàn tiền trước khi hủy'
      })
    }

    const previousStatus = order.status

    const updatedOrder = await prisma.$transaction(async (tx) => {
      // Competes on the same order row as cash/VNPay confirmation.
      const result = await tx.order.updateMany({
        where: {
          id: orderId, status: { in: allowedStatuses }, paidAt: null,
          payments: { none: { status: PaymentStatus.PAID } },
          ...(actorRole === Role.CUSTOMER ? { customerId: actorId } : {})
        },
        data: { status: OrderStatus.CANCELLED, cancelReason: reason, expireAt: null }
      })
      if (result.count === 0) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.CONFLICT,
          message: 'Đơn đã thanh toán hoặc thay đổi trạng thái. Vui lòng tải lại đơn.'
        })
      }
      return tx.order.findUniqueOrThrow({ where: { id: orderId }, include: orderDetailInclude })
    })

    emitOrderStatusUpdate({
      orderId: updatedOrder.id,
      orderType: updatedOrder.type,
      previousStatus,
      status: updatedOrder.status,
      updatedAt: new Date().toISOString(),
      updatedBy: {
        userId: actorId,
        role: actorRole
      }
    })

    await notificationsServices.createOrderStatusNotification({
      orderId: updatedOrder.id,
      previousStatus,
      status: updatedOrder.status,
      actorRole,
      reason
    }).catch((error) => {
      console.error('[Notification] Failed to create cancellation notification:', error)
    })

    return updatedOrder
  }
}

const ordersServices = new OrdersServices()
export default ordersServices
