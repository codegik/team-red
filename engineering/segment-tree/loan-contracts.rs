// Generic segment tree: swap the merge fn + neutral to get min / max / sum range queries. See operations.md.

#![allow(dead_code)] // API is exercised by the tests module; main is only a stub

// 1-indexed array of size 4*n: node 1 = root, children = 2*node and 2*node+1.
struct SegmentTree<T: Clone> {
    tree:    Vec<T>,
    n:       usize,
    neutral: T,
    merge:   fn(&T, &T) -> T,
}

impl<T: Clone> SegmentTree<T> {
    fn new(data: Vec<T>, neutral: T, merge: fn(&T, &T) -> T) -> Self {
        let n = data.len();
        let tree = vec![neutral.clone(); 4 * n];
        let mut st = SegmentTree { tree, n, neutral, merge };
        if n > 0 {
            st.build(&data, 1, 0, n - 1);
        }
        st
    }

    // Merge result over [l, r] in O(log n).
    fn query(&self, l: usize, r: usize) -> T {
        self.query_range(1, 0, self.n - 1, l, r)
    }

    // Set position `pos` and recompute up to the root in O(log n).
    fn update(&mut self, pos: usize, value: T) {
        let n = self.n;
        self.update_range(1, 0, n - 1, pos, value);
    }

    fn root(&self) -> T {
        self.tree[1].clone()
    }

    fn len(&self) -> usize {
        self.n
    }

    fn build(&mut self, data: &[T], node: usize, start: usize, end: usize) {
        if start == end {
            self.tree[node] = data[start].clone();
            return;
        }
        let mid = (start + end) / 2;
        self.build(data, 2 * node,     start,   mid);
        self.build(data, 2 * node + 1, mid + 1, end);
        // clone children first: can't borrow self.tree mutably and immutably at once
        let left  = self.tree[2 * node].clone();
        let right = self.tree[2 * node + 1].clone();
        self.tree[node] = (self.merge)(&left, &right);
    }

    fn query_range(&self, node: usize, start: usize, end: usize, l: usize, r: usize) -> T {
        if r < start || end < l { return self.neutral.clone(); }      // no overlap
        if l <= start && end <= r { return self.tree[node].clone(); } // fully covered
        let mid   = (start + end) / 2;                                // partial: split + merge
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
        if pos <= mid {
            self.update_range(2 * node,     start,   mid, pos, value);
        } else {
            self.update_range(2 * node + 1, mid + 1, end, pos, value);
        }
        let left  = self.tree[2 * node].clone();
        let right = self.tree[2 * node + 1].clone();
        self.tree[node] = (self.merge)(&left, &right);
    }
}


#[derive(Clone)]
struct LoanContract {
    contract_id:    u32,
    borrower:       String,
    amount:         f64,
    days_remaining: i32,
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


// Each neutral uses a sentinel that can never win its own merge, so it stays invisible to queries.
fn neutral_urgent() -> LoanContract { LoanContract::new(0, "-", 0.0, i32::MAX) }
fn merge_urgent(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.days_remaining <= b.days_remaining { a.clone() } else { b.clone() }
}

fn neutral_slack() -> LoanContract { LoanContract::new(0, "-", 0.0, i32::MIN) }
fn merge_slack(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.days_remaining >= b.days_remaining { a.clone() } else { b.clone() }
}

fn neutral_lowest() -> LoanContract { LoanContract::new(0, "-", f64::INFINITY, 0) }
fn merge_lowest(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.amount <= b.amount { a.clone() } else { b.clone() }
}

fn neutral_highest() -> LoanContract { LoanContract::new(0, "-", f64::NEG_INFINITY, 0) }
fn merge_highest(a: &LoanContract, b: &LoanContract) -> LoanContract {
    if a.amount >= b.amount { a.clone() } else { b.clone() }
}

// Sum of two contracts is a scalar, not a contract — this tree stores f64 with neutral 0.0.
fn merge_f64_sum(a: &f64, b: &f64) -> f64 { a + b }


fn main() {
    println!("SegmentTree — run `rustc --test loan-contracts.rs` to verify all use cases.");
}


#[cfg(test)]
mod tests {
    use super::*;

    fn make_urgent_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_urgent(), merge_urgent)
    }

    #[test]
    fn urgent_root_is_eve() {
        let st = make_urgent_tree();
        assert_eq!(st.root().contract_id, 5);
        assert_eq!(st.root().days_remaining, 2);
    }

    #[test]
    fn urgent_query_1_to_4_is_bob() {
        let st = make_urgent_tree();
        let r = st.query(0, 3);
        assert_eq!(r.contract_id, 2);
        assert_eq!(r.days_remaining, 7);
    }

    #[test]
    fn urgent_query_5_to_8_is_eve() {
        let st = make_urgent_tree();
        let r = st.query(4, 7);
        assert_eq!(r.contract_id, 5);
    }

    #[test]
    fn urgent_after_eve_renegotiation_bob_takes_over() {
        let mut st = make_urgent_tree();
        st.update(4, LoanContract::new(5, "Eve", 1_500.00, 90));
        let r = st.query(0, 7);
        assert_eq!(r.contract_id, 2);
        assert_eq!(r.days_remaining, 7);
    }

    fn make_slack_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_slack(), merge_slack)
    }

    #[test]
    fn slack_root_is_grace() {
        let st = make_slack_tree();
        assert_eq!(st.root().contract_id, 7);
        assert_eq!(st.root().days_remaining, 60);
    }

    #[test]
    fn slack_query_1_to_4_is_carol() {
        let st = make_slack_tree();
        let r = st.query(0, 3);
        assert_eq!(r.contract_id, 3);
        assert_eq!(r.days_remaining, 45);
    }

    fn make_lowest_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_lowest(), merge_lowest)
    }

    #[test]
    fn lowest_root_is_eve() {
        let st = make_lowest_tree();
        assert_eq!(st.root().contract_id, 5);
    }

    #[test]
    fn lowest_query_1_to_4_is_carol() {
        let st = make_lowest_tree();
        let r = st.query(0, 3);
        assert_eq!(r.contract_id, 3);
    }

    fn make_highest_tree() -> SegmentTree<LoanContract> {
        SegmentTree::new(init_contracts(), neutral_highest(), merge_highest)
    }

    #[test]
    fn highest_root_is_bob() {
        let st = make_highest_tree();
        assert_eq!(st.root().contract_id, 2);
    }

    #[test]
    fn highest_query_5_to_8_is_frank() {
        let st = make_highest_tree();
        let r = st.query(4, 7);
        assert_eq!(r.contract_id, 6);
    }

    fn make_sum_tree() -> SegmentTree<f64> {
        let amounts: Vec<f64> = init_contracts().iter().map(|c| c.amount).collect();
        SegmentTree::new(amounts, 0.0, merge_f64_sum)
    }

    #[test]
    fn sum_total_portfolio() {
        let st = make_sum_tree();
        assert!((st.root() - 51_800.0).abs() < 0.01);
    }

    #[test]
    fn sum_query_1_to_4() {
        let st = make_sum_tree();
        let total = st.query(0, 3);
        assert!((total - 28_700.0).abs() < 0.01);
    }

    #[test]
    fn sum_query_5_to_8() {
        let st = make_sum_tree();
        let total = st.query(4, 7);
        assert!((total - 23_100.0).abs() < 0.01);
    }

    #[test]
    fn sum_after_eve_partial_payment() {
        let mut st = make_sum_tree();
        st.update(4, 500.00);
        assert!((st.root() - 50_200.0).abs() < 0.01); // 51800 - 2100 + 500
    }

    #[test]
    fn len_reflects_element_count() {
        let st = make_urgent_tree();
        assert_eq!(st.len(), 8);
    }

    #[test]
    fn query_single_index_returns_that_contract() {
        let st = make_urgent_tree();
        let r = st.query(3, 3);
        assert_eq!(r.contract_id, 4);
        assert_eq!(r.days_remaining, 14);
    }
}
