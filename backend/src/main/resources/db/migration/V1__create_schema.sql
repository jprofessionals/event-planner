CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    admin_token UUID NOT NULL,
    secret_votes BOOLEAN NOT NULL DEFAULT FALSE,
    stage VARCHAR(20) NOT NULL DEFAULT 'SCHEDULING' CHECK (stage IN ('SCHEDULING', 'PLANNING')),
    decided_time_start TIMESTAMP WITH TIME ZONE,
    decided_time_end TIMESTAMP WITH TIME ZONE,
    passphrase VARCHAR(255) NOT NULL,
    owner_id UUID REFERENCES users(id),
    participants_can_poll BOOLEAN NOT NULL DEFAULT TRUE,
    participants_can_checklist BOOLEAN NOT NULL DEFAULT TRUE,
    participants_can_shopping_list BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event_participants (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    display_name VARCHAR(100) NOT NULL,
    is_invited BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_id, display_name)
);

CREATE TABLE time_options (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE time_votes (
    id UUID PRIMARY KEY,
    time_option_id UUID NOT NULL REFERENCES time_options(id) ON DELETE CASCADE,
    participant_name VARCHAR(100) NOT NULL,
    user_id UUID REFERENCES users(id),
    vote VARCHAR(10) NOT NULL CHECK (vote IN ('YES', 'MAYBE', 'NO')),
    UNIQUE (time_option_id, participant_name)
);

CREATE TABLE polls (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    question VARCHAR(500) NOT NULL,
    allow_multiple BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poll_options (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    text VARCHAR(255) NOT NULL
);

CREATE TABLE poll_votes (
    id UUID PRIMARY KEY,
    poll_option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    participant_name VARCHAR(100) NOT NULL,
    user_id UUID REFERENCES users(id),
    UNIQUE (poll_option_id, participant_name)
);

CREATE TABLE checklist_items (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    text VARCHAR(500) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_to VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_name VARCHAR(100) NOT NULL,
    user_id UUID REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event_shopping_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    share_token VARCHAR(255) NOT NULL,
    widget_url VARCHAR(500) NOT NULL,
    created_by_participant VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_admin_token ON events(admin_token);
CREATE INDEX idx_event_participants_event_id ON event_participants(event_id);
CREATE INDEX idx_time_options_event_id ON time_options(event_id);
CREATE INDEX idx_time_votes_time_option_id ON time_votes(time_option_id);
CREATE INDEX idx_polls_event_id ON polls(event_id);
CREATE INDEX idx_poll_options_poll_id ON poll_options(poll_id);
CREATE INDEX idx_poll_votes_poll_option_id ON poll_votes(poll_option_id);
CREATE INDEX idx_checklist_items_event_id ON checklist_items(event_id);
CREATE INDEX idx_comments_event_id ON comments(event_id);
CREATE INDEX idx_event_shopping_lists_event_id ON event_shopping_lists(event_id);
