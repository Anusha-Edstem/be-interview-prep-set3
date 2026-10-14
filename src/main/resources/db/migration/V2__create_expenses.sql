CREATE TABLE expenses (
    id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    category VARCHAR(16) NOT NULL,
    expense_date DATE NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_expenses_category CHECK (category IN ('FOOD', 'TRAVEL', 'BILLS', 'OTHER'))
);

CREATE INDEX ix_expenses_expense_date ON expenses (expense_date);

CREATE INDEX ix_expenses_category_expense_date ON expenses (category, expense_date);
