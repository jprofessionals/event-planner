package com.meet.config

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import javax.sql.DataSource

@ApplicationScoped
class JooqProducer {
    @Produces
    @ApplicationScoped
    fun dslContext(dataSource: DataSource): DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)
}
