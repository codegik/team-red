# Segment Tree Kata

This project demonstrates how the same Segment Tree implementation can solve
different range-query problems by changing only two things:

- `merge`: how two child nodes are combined.
- `neutral`: the identity value returned when a node is outside the query range.

## Use Case

The main example models a loan portfolio. Each array position stores a
`LoanContract` with:

- contract id
- borrower
- amount
- days remaining
- due date

The Segment Tree can answer portfolio questions over the whole set or over any
subrange:

- Which contract is the most urgent by remaining days?
- Which contract has the most slack by remaining days?
- Which contract has the lowest or highest amount?
- Which contract has the earliest or latest due date?
- What is the total amount in a portfolio slice?

The date example uses a small `SimpleDate` type instead of an external crate.
Because it derives `Ord`, the tree can compare dates directly and return the
contract with the earliest or latest due date.

## Why This Version Is Useful

This is not just a numeric sum/min/max tree. The tree can store domain objects
and return the object that wins a business rule. For example, an operations
dashboard can ask:

> Among contracts 2000 through 4000, which contract is due first?

Without a Segment Tree, this requires scanning every contract in that range.
With this implementation, range queries and single-contract updates are both
`O(log n)` after an `O(n)` build.

## Pros

- One generic tree supports several business rules.
- Queries and updates are fast: `O(log n)`.
- The returned value can be the full contract, not just the compared scalar.
- Date-based queries work without adding third-party dependencies.

## Cons

- Each query rule needs its own `merge` and `neutral` pair.
- If you need several query rules at the same time, you usually build one tree
  per rule.
- The included `SimpleDate` is intentionally minimal. Production code should
  validate dates or use a proper date/time crate.

## Usage

### Run

```bash
cargo run
```

Expected output:

```text
Loan contracts module running
Most urgent contract => id=5, borrower=Eve, days_remaining=2, due_date=2026-05-30
Earliest due contract => id=5, borrower=Eve, due_date=2026-05-30
```

### Tests

```bash
cargo test
```

Current test coverage includes the original numeric examples plus the loan
contract examples:

```text
test result: ok. 32 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out
```


```bash
cargo test

test loan_contracts::tests::highest_query_5_to_8_is_frank ... ok
test loan_contracts::tests::highest_root_is_bob ... ok
test loan_contracts::tests::lowest_query_1_to_4_is_carol ... ok
test loan_contracts::tests::lowest_root_is_eve ... ok
test loan_contracts::tests::len_reflects_element_count ... ok
test loan_contracts::tests::query_single_index_returns_that_contract ... ok
test loan_contracts::tests::slack_query_1_to_4_is_carol ... ok
test loan_contracts::tests::sum_after_eve_partial_payment ... ok
test loan_contracts::tests::slack_root_is_grace ... ok
test loan_contracts::tests::sum_query_1_to_4 ... ok
test loan_contracts::tests::sum_query_5_to_8 ... ok
test loan_contracts::tests::sum_total_portfolio ... ok
test loan_contracts::tests::urgent_after_eve_renegotiation_bob_takes_over ... ok
test loan_contracts::tests::urgent_query_1_to_4_is_bob ... ok
test loan_contracts::tests::urgent_query_5_to_8_is_eve ... ok
test loan_contracts::tests::urgent_root_is_eve ... ok
test simple_operations::tests::max_full_range ... ok
test simple_operations::tests::max_single_element ... ok
test simple_operations::tests::max_students_3_to_6 ... ok
test simple_operations::tests::max_students_6_to_8 ... ok
test simple_operations::tests::max_update_student5 ... ok
test simple_operations::tests::min_10h_to_16h ... ok
test simple_operations::tests::min_full_day ... ok
test simple_operations::tests::min_single_element ... ok
test simple_operations::tests::min_update_reading_10h ... ok
test simple_operations::tests::sum_days_3_to_6 ... ok
test simple_operations::tests::sum_full_range ... ok
test simple_operations::tests::sum_update_day4 ... ok

test result: ok. 28 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
```
