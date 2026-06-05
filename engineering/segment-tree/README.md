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

## Solution Type

This is an **online, indexed, binary range-tree solution**.

It is a good fit when data changes over time and the application needs to alternate between point updates and frequent range queries. Instead of recomputing a full range on every query, the tree stores partial aggregate results in its internal nodes.

Complexities:

* Initial build: `O(n)`.
* Range query: `O(log n)` on average for ranges that are well partitioned by the tree.
* Point update: `O(log n)`.
* Memory: `O(n)`, implemented as a `Vec<T>` allocated with `4 * n` slots.

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

## Improvement Points

* Return `Option<T>` or `Result<T, SegmentTreeError>` for invalid queries and updates.
* Handle empty trees explicitly.
* Replace the `fn` pointer with a generic `F: Fn(&T, &T) -> T`, allowing closures and stateful strategies.
* Reduce cloning by using references, `Copy` where applicable, or storing indexes for large objects.
* Move the generic tree implementation into its own module and keep `loan_contracts` as a domain example.
* Add an explicit tie-break policy for contracts with the same deadline or amount.
* Replace `f64` with cents stored as `i64`, or use a decimal type in a real financial scenario.
* Add lazy propagation for range updates.
* Add tests for invalid indexes, empty input, and tie cases.
* Consider an iterative implementation to reduce recursion and simplify boundary handling.

## Usage

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
```

Current result:

```text
test result: ok. 16 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
```
