package com.trioForce.voyage.aftersale

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.order.OrderRepository
import com.trioForce.voyage.security.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * 售后服务：
 * 负责售后工单的创建、查询与后台状态更新。
 */
@Service
class AfterSaleService(
    private val afterSaleRepository: AfterSaleRepository,
    private val orderRepository: OrderRepository
) {
    /**
     * 用户创建售后工单。
     *
     * @param req 售后申请请求，包含订单号与问题描述
     */
    @Transactional
    fun create(req: CreateAfterSaleRequest) {
        val userId = CurrentUser.userId()
        val order = orderRepository.findByOrderNo(req.orderNo).orElseThrow { BizException("order not found") }
        if (order.userId != userId) throw BizException("forbidden order")
        afterSaleRepository.save(
            AfterSaleEntity(
                userId = userId,
                orderNo = req.orderNo.trim(),
                content = req.content.trim(),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        )
    }

    /**
     * 后台代客发起售后：工单归属仍为下单用户（[AfterSaleEntity.userId] = 订单买家）。
     */
    @Transactional
    fun adminCreate(req: CreateAfterSaleRequest) {
        val order = orderRepository.findByOrderNo(req.orderNo.trim()).orElseThrow { BizException("order not found") }
        afterSaleRepository.save(
            AfterSaleEntity(
                userId = order.userId,
                orderNo = req.orderNo.trim(),
                content = req.content.trim(),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        )
    }

    /**
     * 查询当前登录用户的售后工单列表。
     *
     * @return 当前用户售后工单
     */
    fun listMine(): List<AfterSaleView> =
        afterSaleRepository.findAllByUserIdOrderByIdDesc(CurrentUser.userId()).map {
            AfterSaleView(it.id!!, it.orderNo, it.status, it.content)
        }

    /**
     * 后台查询所有售后工单。
     *
     * @return 全量售后工单
     */
    fun listAll(): List<AfterSaleView> =
        afterSaleRepository.findAll().sortedByDescending { it.id }.map {
            AfterSaleView(it.id!!, it.orderNo, it.status, it.content)
        }

    /**
     * 后台更新售后工单状态。
     *
     * @param id 售后工单 ID
     * @param status 新状态，例如 PROCESSING/RESOLVED/CLOSED
     */
    @Transactional
    fun updateStatus(id: Long, status: String) {
        val entity = afterSaleRepository.findById(id).orElseThrow { BizException("after sale not found") }
        entity.status = status.trim().uppercase()
        entity.updatedAt = OffsetDateTime.now()
        afterSaleRepository.save(entity)
    }
}
