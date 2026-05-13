package com.trioForce.voyage.product

import com.trioForce.voyage.common.logging.LogUtil
import com.trioForce.voyage.common.snowflake.SnowflakeIdGenerator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 绑定 `voyage.snowflake.*`，供多实例部署时区分 worker/datacenter 位段。 */
@ConfigurationProperties(prefix = "voyage.snowflake")
data class SnowflakeProperties(
    var workerId: Long = 1L,
    var datacenterId: Long = 1L,
)

@Configuration
@EnableConfigurationProperties(SnowflakeProperties::class)
class SnowflakeConfiguration {
    private val log = LogUtil.logger<SnowflakeConfiguration>()

    @Bean
    fun snowflakeIdGenerator(props: SnowflakeProperties): SnowflakeIdGenerator {
        log.info(
            "snowflake bean init workerId={} datacenterId={}",
            props.workerId,
            props.datacenterId,
        )
        return SnowflakeIdGenerator(workerId = props.workerId, datacenterId = props.datacenterId)
    }
}
