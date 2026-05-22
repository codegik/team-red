// loan-contracts.rs
//
// Segment Tree where each node holds a LoanContract (struct with multiple
// attributes), not a scalar. The same generic tree answers 5 different
// business questions by swapping only the merge function and neutral element.
//
//   Use case 1 - MOST URGENT      (min by days)   -> contract to collect first
//   Use case 2 - MOST SLACK       (max by days)   -> contract with most time
//   Use case 3 - LOWEST BALANCE   (min by amount) -> smallest individual debt
//   Use case 4 - HIGHEST EXPOSURE (max by amount) -> largest individual debt
//   Use case 5 - TOTAL EXPOSURE   (sum by amount) -> total portfolio balance
//
// Why there is no "sum by days":
//   Due dates run in parallel, not in series. If A is due in 7 days and B in 30,
//   A is paid on day 7 WITHIN B's window — you don't wait 37 days for anything.
//   Summing individual deadlines produces a number with no real business unit.
//   See operations.md for details.
//
// How to run:
//   rustc loan-contracts.rs && ./loan-contracts
//
// How to run tests:
//   rustc --test loan-contracts.rs -o loan-contracts-test && ./loan-contracts-test

// The API is exercised by the tests module; `main` is only a stub, so a
// plain binary build would otherwise flag every item as dead code.
#![allow(dead_code)]


// ==========================================================
// STRUCT SegmentTree<T>
// ==========================================================
//
// Generic structure: works with any clonable type T.
// Use cases 1-4 use SegmentTree<LoanContract>.
// Use case 5 uses SegmentTree<f64> — the sum of two contracts is a
// scalar, not a contract, so the tree stores f64 directly.
//
// Internal layout: 1-indexed vector of size 4*n.
//   node 1      = root (covers [0, n-1])
//   left child  = 2 * node
//   right child = 2 * node + 1

struct SegmentTree<T: Clone> {
    tree:    Vec<T>,           // internal tree, 1-indexed
    n:       usize,            // number of elements
    neutral: T,                // neutral element for merge
    merge:   fn(&T, &T) -> T,  // combination operation
}

impl<T: Clone> SegmentTree<T> {

    // Builds the tree from `data`, `neutral` and `merge`.
    // Complexity: O(n).
    fn new(data: Vec<T>, neutral: T, merge: fn(&T, &T) -> T) -> Self {
        let n = data.len();
        let tree = vec![neutral.clone(); 4 * n];
        let mut st = SegmentTree { tree, n, neutral, merge };
        if n > 0 {
            st.build(&data, 1, 0, n - 1);
        }
        st
    }

    // Returns the merge result for the interval [l, r].
    // Complexity: O(log n).
    fn query(&self, l: usize, r: usize) -> T {
        self.query_range(1, 0, self.n - 1, l, r)
    }

    // Updates the value at position `pos` and recalculates up to the root.
    // Complexity: O(log n).
    fn update(&mut self, pos: usize, value: T) {
        let n = self.n;
        self.update_range(1, 0, n - 1, pos, value);
    }

    // Value at the root = merge result of all elements.
    fn root(&self) -> T {
        self.tree[1].clone()
    }

    // Number of elements.
    fn len(&self) -> usize {
        self.n
    }

    // ---- internal (recursive) methods ----

    fn build(&mut self, data: &[T], node: usize, start: usize, end: usize) {
        if start == end {
            self.tree[node] = data[start].clone();
            return;
        }
        let mid = (start + end) / 2;
        self.build(data, 2 * node,     start,   mid);
        self.build(data, 2 * node + 1, mid + 1, end);
        // clone before merging to avoid borrow conflict on self.tree
        let left  = self.tree[2 * node].clone();
        let right = self.tree[2 * node + 1].clone();
        self.tree[node] = (self.merge)(&left, &right);
    }

    fn query_range(&self, node: usize, start: usize, end: usize, l: usize, r: usize) -> T {
        // CASE 1: range outside the query -> return neutral
        if r < start || end < l { return self.neutral.clone(); }
        // CASE 2: range fully inside the query -> answer is at this node
        if l <= start && end <= r { return self.tree[node].clone(); }
        // CASE 3: partial overlap -> descend and combine
        let mid   = (start + end) / 2;
        let left  = self.query_range(2 * node,     start,   mid, l, r);
        let right = self.query_range(2 * node + 1, mid + 1, end, l, r);
        (self.merge)(&left, &right)
    }

    fn update_range(&mut self, node: usize, start: usize, end: usize, pos: usize, value: T) {
        if start == end {
            self.tree[node] = value;
            return;
        }
        let mid = (start + end) / 2;
        // descend only into the side that contains pos
        if pos <= mid {
            self.update_range(2 * node,     start,   mid, pos, value);
        } else {
            self.update_range(2 * node + 1, mid + 1, end, pos, value);
        }
        // on the way back up, recalculate this node from its children
        let left  = self.tree[2 * node].clone();
        let right = self.tree[2 * node + 1].clone();
        self.tree[node] = (self.merge)(&left, &right);
    }
}


// ==========================================================
// DOMAIN MODEL: LoanContract
// ==========================================================

#[derive(Clone)]
struct LoanContract {
    contract_id:    u32,
    borrower:       String,
    amount:         f64,   // outstanding balance (R$)
    days_remaining: i32,   // days until due date
}

impl LoanContract {
    fn new(contract_id: u32, borrower: &str, amount: f64, days_remaining: i32) -> Self {
        LoanContract {
            contract_id,
            borrower: borrower.to_string(),
            amount,
            days_remaining,
        }
    }
}

fn init_contracts() -> Vec<LoanContract> {
    vec![
        LoanContract::new(1, "Alice",  5_000.00, 30),
        LoanContract::new(2, "Bob",   12_000.00,  7),
        LoanContract::new(3, "Carol",  3_200.00, 45),
        LoanContract::new(4, "David",  8_500.00, 14),
        LoanContract::new(5, "Eve",    2_100.00,  2),
        LoanContract::new(6, "Frank",  9_900.00, 21),
        LoanContract::new(7, "Grace",  6_700.00, 60),
        LoanContract::new(8, "Hank",   4_400.00,  9),
    ]
}


// ==========================================================
// Merge and neutral per use case
// ==========================================================
//
// Business logic (which field, which criterion) lives in one place.
// If the criterion changes, only the merge_* function needs editing.
//
// Neutrals are functions (not constants) because each call needs
// independent ownership — Rust does not allow shared references here.

// --- Use case 1: MOST URGENT (min by days) ---
// Neutral: days = i32::MAX -> never wins a min, invisible to queries
fn neutral_urgent() -> LoanContract {
    LoanContract::new(0, "-", 0.0, i32::MAX)
}
fn merge_urgent(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.days_remaining <= b.days_remaining { a.clone() } else { b.clone() }
}

// --- Use case 2: MOST SLACK (max by days) ---
// Neutral: days = i32::MIN -> never wins a max
fn neutral_slack() -> LoanContract {
    LoanContract::new(0, "-", 0.0, i32::MIN)
}
fn merge_slack(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.days_remaining >= b.days_remaining { a.clone() } else { b.clone() }
}

// --- Use case 3: LOWEST BALANCE (min by amount) ---
// Neutral: amount = f64::INFINITY -> never wins a min
fn neutral_lowest() -> LoanContract {
    LoanContract::new(0, "-", f64::INFINITY, 0)
}
fn merge_lowest(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.amount <= b.amount { a.clone() } else { b.clone() }
}

// --- Use case 4: HIGHEST EXPOSURE (max by amount) ---
// Neutral: amount = f64::NEG_INFINITY -> never wins a max
fn neutral_highest() -> LoanContract {
    LoanContract::new(0, "-", f64::NEG_INFINITY, 0)
}
fn merge_highest(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.amount >= b.amount { a.clone() } else { b.clone() }
}

// --- Use case 5: TOTAL EXPOSURE (sum by amount) ---
// The tree stores f64 (scalar), not LoanContract.
// The sum of two contracts is a number, not a contract.
// Neutral: 0.0 (additive identity)
fn merge_f64_sum(a: &f64, b: &f64) -> f64 { a + b }


// ==========================================================
// main
// ==========================================================
//
// The behavior of every use case is verified in the tests module below.
// Run them with: rustc --test loan-contracts.rs -o t && ./t
fn main() {
    println!("SegmentTree — run `rustc --test loan-contracts.rs` to verify all use cases.");
}


// ==========================================================
// UNIT TESTS
// ==========================================================
//
// rustc --test loan-contracts.rs -o loan-contracts-test && ./loan-contracts-test

#[cfg(test)]
mod tests {
    use super::*;

    // portfolio: Alice(1,5000,30) Bob(2,12000,7) Carol(3,3200,45) David(4,8500,14)
    //            Eve(5,2100,2)   Frank(6,9900,21) Grace(7,6700,60) Hank(8,4400,9)

    // ----------------------------------------------------------
    // Use case 1: MOST URGENT (min by days)
    // ----------------------------------------------------------

    fn make_urgent_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_urgent(), merge_urgent)
    }

    #[test]
    fn urgent_root_is_eve() {
        let st = make_urgent_tree();
        assert_eq!(st.root().contract_id, 5); // Eve - 2 days
        assert_eq!(st.root().days_remaining, 2);
    }

    #[test]
    fn urgent_query_1_to_4_is_bob() {
        let st = make_urgent_tree();
        // Alice(30) Bob(7) Carol(45) David(14) -> Bob is most urgent
        let r = st.query(0, 3);
        assert_eq!(r.contract_id, 2);
        assert_eq!(r.days_remaining, 7);
    }

    #[test]
    fn urgent_query_5_to_8_is_eve() {
        let st = make_urgent_tree();
        // Eve(2) Frank(21) Grace(60) Hank(9) -> Eve is most urgent
        let r = st.query(4, 7);
        assert_eq!(r.contract_id, 5);
    }

    #[test]
    fn urgent_after_eve_renegotiation_bob_takes_over() {
        let mut st = make_urgent_tree();
        st.update(4, LoanContract::new(5, "Eve", 1_500.00, 90));
        // Eve now has 90 days; Bob (7) becomes most urgent
        let r = st.query(0, 7);
        assert_eq!(r.contract_id, 2);
        assert_eq!(r.days_remaining, 7);
    }

    // ----------------------------------------------------------
    // Use case 2: MOST SLACK (max by days)
    // ----------------------------------------------------------

    fn make_slack_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_slack(), merge_slack)
    }

    #[test]
    fn slack_root_is_grace() {
        let st = make_slack_tree();
        assert_eq!(st.root().contract_id, 7); // Grace - 60 days
        assert_eq!(st.root().days_remaining, 60);
    }

    #[test]
    fn slack_query_1_to_4_is_carol() {
        let st = make_slack_tree();
        // Alice(30) Bob(7) Carol(45) David(14) -> Carol has most slack
        let r = st.query(0, 3);
        assert_eq!(r.contract_id, 3);
        assert_eq!(r.days_remaining, 45);
    }

    // ----------------------------------------------------------
    // Use case 3: LOWEST BALANCE (min by amount)
    // ----------------------------------------------------------

    fn make_lowest_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_lowest(), merge_lowest)
    }

    #[test]
    fn lowest_root_is_eve() {
        let st = make_lowest_tree();
        assert_eq!(st.root().contract_id, 5); // Eve - R$2,100
    }

    #[test]
    fn lowest_query_1_to_4_is_carol() {
        let st = make_lowest_tree();
        // Alice(5000) Bob(12000) Carol(3200) David(8500) -> Carol lowest balance
        let r = st.query(0, 3);
        assert_eq!(r.contract_id, 3);
    }

    // ----------------------------------------------------------
    // Use case 4: HIGHEST EXPOSURE (max by amount)
    // ----------------------------------------------------------

    fn make_highest_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_highest(), merge_highest)
    }

    #[test]
    fn highest_root_is_bob() {
        let st = make_highest_tree();
        assert_eq!(st.root().contract_id, 2); // Bob - R$12,000
    }

    #[test]
    fn highest_query_5_to_8_is_frank() {
        let st = make_highest_tree();
        // Eve(2100) Frank(9900) Grace(6700) Hank(4400) -> Frank highest exposure
        let r = st.query(4, 7);
        assert_eq!(r.contract_id, 6);
    }

    // ----------------------------------------------------------
    // Use case 5: TOTAL EXPOSURE (sum by amount)
    // ----------------------------------------------------------

    fn make_sum_tree() -> SegmentTree<f64> {
        let amounts: Vec<f64> = init_contracts().iter().map(|c| c.amount).collect();
        SegmentTree::new(amounts, 0.0, merge_f64_sum)
    }

    #[test]
    fn sum_total_portfolio() {
        let st = make_sum_tree();
        // 5000+12000+3200+8500+2100+9900+6700+4400 = 51800
        assert!((st.root() - 51_800.0).abs() < 0.01);
    }

    #[test]
    fn sum_query_1_to_4() {
        let st = make_sum_tree();
        // 5000+12000+3200+8500 = 28700
        let total = st.query(0, 3);
        assert!((total - 28_700.0).abs() < 0.01);
    }

    #[test]
    fn sum_query_5_to_8() {
        let st = make_sum_tree();
        // 2100+9900+6700+4400 = 23100
        let total = st.query(4, 7);
        assert!((total - 23_100.0).abs() < 0.01);
    }

    #[test]
    fn sum_after_eve_partial_payment() {
        let mut st = make_sum_tree();
        // Eve partially paid: new balance R$500
        st.update(4, 500.00);
        // 51800 - 2100 + 500 = 50200
        assert!((st.root() - 50_200.0).abs() < 0.01);
    }

    // ----------------------------------------------------------
    // Generic API: len and single-index query
    // ----------------------------------------------------------

    #[test]
    fn len_reflects_element_count() {
        let st = make_urgent_tree();
        assert_eq!(st.len(), 8);
    }

    #[test]
    fn query_single_index_returns_that_contract() {
        let st = make_urgent_tree();
        // query(i, i) must return exactly the contract at index i
        let r = st.query(3, 3); // David
        assert_eq!(r.contract_id, 4);
        assert_eq!(r.days_remaining, 14);
    }
}
