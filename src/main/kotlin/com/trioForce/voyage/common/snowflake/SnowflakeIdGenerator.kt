package com.trioForce.voyage.common.snowflake

import com.trioForce.voyage.common.logging.LogUtil

/**
 * 简化版 Twitter Snowflake 风格发号器，输出 **64 位有符号 Long**。
 *
 * ## 为何存在
 * - 商品对外主键不再用数据库自增小整数，避免可枚举、可猜测；改用时间有序的全局唯一 ID。
 * - **JSON/URL 中一律用十进制字符串**（见 [nextIdString]），避免 JavaScript `Number` 超过 `MAX_SAFE_INTEGER` 丢精度。
 *
 * ## 位布局（自高位至低位）
 * - 41 位：毫秒时间戳相对 [EPOCH_MS] 的偏移（约 69 年可用量，取决于 epoch 选取）。
 * - 5 位：数据中心 ID（0–31），多机房/多实例部署时须保证组合唯一，见配置 `voyage.snowflake.datacenter-id`。
 * - 5 位：机器/Worker ID（0–31），同数据中心内多 JVM 实例须各不相同，见 `voyage.snowflake.worker-id`。
 * - 12 位：毫秒内序列（0–4095）；同一毫秒内溢出则自旋等到下一毫秒。
 *
 * ## 运维注意
 * - **时钟回拨**会抛异常；抛出前通过 [LogUtil] 记录 WARN，禁止用标准输出排查。
 * - 单机单实例用默认 worker/datacenter 即可；水平扩展时 **必须为每个进程分配唯一 (dc, worker)**，否则可能重复。
 */
class SnowflakeIdGenerator(
    /** 机器号，参与位运算前会被限制在 0..31。 */
    private val workerId: Long = 1L,
    /** 数据中心号，参与位运算前会被限制在 0..31。 */
    private val datacenterId: Long = 1L,
) {
    /** 同一 JVM 内串行化发号，保证线程安全。 */
    private val lock = Any()

    /** 上次生成 ID 时使用的毫秒时间戳。 */
    private var lastTs = -1L

    /** 同一毫秒内的递增序列（12 位，4096 个/毫秒上限）。 */
    private var seq = 0L

    /** 时钟回拨等异常路径通过 [LogUtil] 打日志，禁止 `println`。 */
    private val log = LogUtil.logger<SnowflakeIdGenerator>()

    /**
     * 生成下一个雪花 ID（Long）。数值可能超过 JS 安全整数范围，**不要**在前端当 number 用。
     */
    fun nextId(): Long {
        synchronized(lock) {
            var ts = System.currentTimeMillis()
            // 时钟异常：比上次还早说明发生了回拨，直接失败以免 ID 重复
            if (ts < lastTs) {
                log.warn(
                    "snowflake clock moved backwards lastTs={} currentTs={} workerId={} datacenterId={}",
                    lastTs,
                    ts,
                    workerId,
                    datacenterId,
                )
                throw IllegalStateException("clock moved backwards")
            }
            if (ts == lastTs) {
                // 同一毫秒内：序列 +1，4096 用满则阻塞到下一毫秒
                seq = (seq + 1) and 4095L
                if (seq == 0L) {
                    while (ts <= lastTs) {
                        ts = System.currentTimeMillis()
                    }
                }
            } else {
                // 新毫秒：序列归零
                seq = 0L
            }
            lastTs = ts
            val wid = workerId.coerceIn(0L, 31L)
            val dc = datacenterId.coerceIn(0L, 31L)
            return ((ts - EPOCH_MS) shl 22) or (dc shl 17) or (wid shl 12) or seq
        }
    }

    /** 对外 API 落库字段推荐使用字符串，避免跨语言精度问题。 */
    fun nextIdString(): String = nextId().toString()

    private companion object {
        /**
         * 自定义纪元（毫秒）。早于该时刻的时间戳无法编码（一般不需要改）。
         * 当前值：2024-01-01 UTC 附近，用于拉长相对时间戳的有效位寿命。
         */
        private const val EPOCH_MS = 1704067200000L
    }
}
