package db.migration

import com.trioForce.voyage.common.logging.LogUtil
import com.trioForce.voyage.common.snowflake.SnowflakeIdGenerator
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * Flyway 版本迁移：为 [t_products.public_id] 补数据。
 *
 * - 执行时机：在 [V16__product_public_snowflake_id] 加列之后、[V18__product_public_id_not_null] 设非空之前；
 * - 每条历史行调用 [SnowflakeIdGenerator] 生成唯一字符串；
 * - **日志**：通过 [LogUtil] 记录回填条数，禁止 `System.out.println`。
 */
@Suppress("unused")
class V17__BackfillProductPublicId : BaseJavaMigration() {
    private val log = LogUtil.logger(V17__BackfillProductPublicId::class.java)

    override fun migrate(context: Context) {
        val gen = SnowflakeIdGenerator(workerId = 1L, datacenterId = 1L)
        val conn = context.connection
        val rs = conn.createStatement().executeQuery(
            "SELECT id FROM t_products WHERE public_id IS NULL ORDER BY id",
        )
        val ids = mutableListOf<Long>()
        while (rs.next()) {
            ids.add(rs.getLong(1))
        }
        rs.close()
        if (ids.isEmpty()) {
            log.info("V17 backfill public_id: no rows to update")
            return
        }
        log.info("V17 backfill public_id: start count={}", ids.size)
        val ps = conn.prepareStatement("UPDATE t_products SET public_id = ? WHERE id = ?")
        for (id in ids) {
            ps.setString(1, gen.nextId().toString())
            ps.setLong(2, id)
            ps.executeUpdate()
        }
        ps.close()
        log.info("V17 backfill public_id: done count={}", ids.size)
    }
}
