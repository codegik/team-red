# Segment Tree Kata

## Use Case

This Rust solution implements a **generic Segment Tree** for answering range queries over a loan contract portfolio.

The main example uses contracts with `amount` and `days_remaining` to answer questions such as:

* Which contract is the most urgent in a range?
* Which contract has the most remaining slack?
* Which contract has the lowest or highest amount?
* What is the total amount for a portfolio slice?
* How can a contract be updated while recalculating only the affected aggregates?

The core structure is `SegmentTree<T>`, parameterized by the stored data type. The tree receives:

* `data`: the initial vector of elements.
* `neutral`: the neutral element returned for segments outside a query.
* `merge`: the function used to combine two child nodes.

With this design, the same structure supports different aggregations: minimum by deadline, maximum by deadline, lowest amount, highest amount, and amount sum.


## Algorithm

The tree recursively splits the original vector into segments. Each node represents a `[start, end]` interval.

* Leaf nodes represent individual elements.
* Internal nodes store the result of `merge(left, right)`.
* A query returns a node value when the node segment is fully inside the requested range.
* When a segment is outside the requested range, the query returns the neutral element.
* When there is partial overlap, the query visits both children and merges their results.
* An update replaces one leaf and recomputes only the path back to the root.

The key requirement is that `merge` must be associative and compatible with `neutral`. Examples:

* Sum: `0.0` as the neutral element.
* Earliest deadline: sentinel contract with `i32::MAX`.
* Latest deadline: sentinel contract with `i32::MIN`.
* Lowest amount: sentinel contract with `f64::INFINITY`.
* Highest amount: sentinel contract with `f64::NEG_INFINITY`.

## Pros

* Range queries run in `O(log n)` without scanning every element in the requested interval.
* Efficient point updates, recalculating only `O(log n)` nodes.
* Flexible aggregation operations across nodes and levels, allowing the same generic tree to support sum, min/max, and object selection.
* The solution uses only standard Rust and has no external dependencies.
* The `Vec<T>` storage improves memory locality compared with a pointer-based tree.
* The code is small enough for a kata while still modeling a realistic domain case.
* Tests cover root aggregation, partial queries, single-index queries, sum aggregation, and updates.
* The loan contract model shows that Segment Tree is not limited to primitive numeric arrays.
* Query and update costs are predictable, which is useful for read-heavy workloads with occasional contract changes.

## Cons

* The current implementation does not validate bounds for `query(l, r)` or `update(pos, value)`.
* Calling `query` on an empty tree would underflow when calculating `self.n - 1`.
* The tree allocates `4 * n` slots, which is simple but can waste memory.
* Each `merge` clones values, which can become expensive when `T` is large or allocation-heavy.
* `merge` is a function pointer (`fn(&T, &T) -> T`), so it cannot capture state like a closure.
* The structure does not expose errors; it currently assumes correct index usage.
* Ties are resolved implicitly by choosing the left side in some merge functions, with no explicit tie-break policy.
* Using `f64` for money is fine for a kata but not appropriate for a real financial domain.
* The solution supports point updates only, not range updates.
* There is no lazy propagation, so operations like "add X to all contracts in a range" are not optimized.
* The tree size is static; inserting or removing contracts requires rebuilding the structure.
* `SegmentTree<T>` is private inside the module, which limits reuse outside the example.

### Run

```bash
cargo run
```

Expected output:

```text
Loan contracts module running
Most urgent contract => id=5, borrower=Eve, days_remaining=2
```

### Tests

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
