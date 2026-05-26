# Segment Tree Kata

### Use Case

### Pros

### Cons

#### Usage

##### Run
```bash
cargo run

Loan contracts module running
Most urgent contract => id=5, borrower=Eve, days_remaining=2
```

##### Tests
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