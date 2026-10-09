import { prisma } from "~/config/prisma"
import HTTP_STATUS from "~/constants/httpStatus"
import { OrderStatus, PaymentMethod, PaymentStatus } from "~/generated/prisma/enums"
import { ErrorWithStatus } from "~/models/Errors"

class PaymentServices {
  async detailPayment(orderId: string) {
    const data = await prisma.payment.findFirst({
      where: { orderId },
      select: { orderId: true, method: true, status: true, amount: true, paidAt: true, createdAt: true }
    })
    return data
  }

  async cashConfirm({ orderId, staffId }: { orderId: string, staffId: string }) {
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      include: { payments: true }
    })

    if (!order || !order.payments.length) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.BAD_REQUEST,
        message: 'Đơn hàng không tồn tại'
      })
    }

    const orderPayment = order.payments.find((payment) => payment.method === PaymentMethod.CASH)

    if (!orderPayment) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.BAD_REQUEST,
        message: 'Đơn này không phải tiền mặt'
      })
    }

    if (order.status !== OrderStatus.PENDING_CONFIRMATION && order.status !== OrderStatus.CONFIRMED) {
      throw new ErrorWithStatus({
        httpStatusCode: HTTP_STATUS.BAD_REQUEST,
        message: `Đơn đang ở trạng thái ${order.status}`
      })
    }

    const paidAt = new Date()

    return prisma.$transaction(async (tx) => {
      const updated = await tx.order.updateMany({
        where: {
          id: orderId,
          status: { in: [OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED] },
          paidAt: null,
          payments: { none: { status: PaymentStatus.PAID } }
        },
        data: { status: OrderStatus.CONFIRMED, paidAt, confirmedById: staffId }
      })
      if (updated.count === 0) {
        throw new ErrorWithStatus({
          httpStatusCode: HTTP_STATUS.CONFLICT,
          message: 'Đơn đã hủy, đã thanh toán hoặc thay đổi trạng thái. Vui lòng tải lại đơn.'
        })
      }

      await tx.payment.update({
        where: { id: orderPayment.id },
        data: {
          status: PaymentStatus.PAID,
          paidAt,
          gatewayData: {
            ...(orderPayment.gatewayData as any),
            confirmCashById: staffId,
            confirmedAt: paidAt.toISOString()
          }
        }
      })
    })
  }

  async applyVnpayResult(orderId: string, paymentId: string, success: boolean, txnRef: string, gatewayData: Record<string, unknown>) {
    return prisma.$transaction(async (tx) => {
      const paidAt = new Date()
      const updated = await tx.order.updateMany({
        where: {
          id: orderId, paidAt: null,
          status: { in: success ? [OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED] : [OrderStatus.PENDING_PAYMENT] },
          payments: { none: { status: PaymentStatus.PAID } }
        },
        // Lock the order row before deciding whether a concurrent cancellation won.
        data: success ? { paidAt, expireAt: null } : { status: OrderStatus.PAYMENT_FAILED }
      })
      if (updated.count === 0) return null
      const order = await tx.order.findUniqueOrThrow({ where: { id: orderId } })
      const requiresRefund = success && order.status === OrderStatus.CANCELLED
      await tx.payment.update({
        where: { id: paymentId },
        data: {
          status: success ? PaymentStatus.PAID : PaymentStatus.FAILED,
          ...(success ? { paidAt, txnRef } : {}),
          gatewayData: { ...gatewayData, ...(requiresRefund ? { requiresRefund: true } : {}) } as any
        }
      })
      if (success && !requiresRefund) {
        await tx.order.update({ where: { id: orderId }, data: { status: OrderStatus.PENDING_CONFIRMATION } })
      }
      return requiresRefund ? OrderStatus.CANCELLED : success ? OrderStatus.PENDING_CONFIRMATION : OrderStatus.PAYMENT_FAILED
    })
  }
}

const paymentServices = new PaymentServices()
export default paymentServices
