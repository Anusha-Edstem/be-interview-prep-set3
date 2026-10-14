CREATE TABLE books (
    id             UUID         NOT NULL,
    title          VARCHAR(255) NOT NULL,
    author         VARCHAR(255) NOT NULL,
    isbn           VARCHAR(13)  NOT NULL,
    published_year INTEGER      NOT NULL,
    borrowed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_books PRIMARY KEY (id),
    CONSTRAINT uk_books_isbn UNIQUE (isbn)
);

CREATE INDEX idx_books_title ON books (title);

CREATE TABLE loans (
    id          UUID         NOT NULL,
    book_id     UUID         NOT NULL,
    member_id   VARCHAR(100) NOT NULL,
    borrowed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    returned_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_loans PRIMARY KEY (id),
    CONSTRAINT fk_loans_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
);

CREATE INDEX idx_loans_book_id ON loans (book_id);
