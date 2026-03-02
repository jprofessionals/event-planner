package com.meet.config

import com.meet.generated.jooq.tables.pojos.ChecklistItems
import com.meet.generated.jooq.tables.pojos.Comments
import com.meet.generated.jooq.tables.pojos.EventParticipants
import com.meet.generated.jooq.tables.pojos.EventShoppingLists
import com.meet.generated.jooq.tables.pojos.Events
import com.meet.generated.jooq.tables.pojos.PollOptions
import com.meet.generated.jooq.tables.pojos.PollVotes
import com.meet.generated.jooq.tables.pojos.Polls
import com.meet.generated.jooq.tables.pojos.TimeOptions
import com.meet.generated.jooq.tables.pojos.TimeVotes
import com.meet.generated.jooq.tables.pojos.Users
import com.meet.generated.jooq.tables.records.ChecklistItemsRecord
import com.meet.generated.jooq.tables.records.CommentsRecord
import com.meet.generated.jooq.tables.records.EventParticipantsRecord
import com.meet.generated.jooq.tables.records.EventShoppingListsRecord
import com.meet.generated.jooq.tables.records.EventsRecord
import com.meet.generated.jooq.tables.records.PollOptionsRecord
import com.meet.generated.jooq.tables.records.PollVotesRecord
import com.meet.generated.jooq.tables.records.PollsRecord
import com.meet.generated.jooq.tables.records.TimeOptionsRecord
import com.meet.generated.jooq.tables.records.TimeVotesRecord
import com.meet.generated.jooq.tables.records.UsersRecord
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import javax.sql.DataSource

@ApplicationScoped
@RegisterForReflection(
    targets = [
        UsersRecord::class,
        EventsRecord::class,
        EventParticipantsRecord::class,
        TimeOptionsRecord::class,
        TimeVotesRecord::class,
        PollsRecord::class,
        PollOptionsRecord::class,
        PollVotesRecord::class,
        ChecklistItemsRecord::class,
        CommentsRecord::class,
        EventShoppingListsRecord::class,
        Users::class,
        Events::class,
        EventParticipants::class,
        TimeOptions::class,
        TimeVotes::class,
        Polls::class,
        PollOptions::class,
        PollVotes::class,
        ChecklistItems::class,
        Comments::class,
        EventShoppingLists::class,
    ],
)
class JooqProducer {
    @Produces
    @ApplicationScoped
    fun dslContext(dataSource: DataSource): DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)
}
