// simple-operations.rs
//
// Demonstracao das operacoes basicas de uma Segment Tree.
// Tres casos de uso pra mostrar que a mesma estrutura resolve
// problemas bem diferentes trocando so o "merge" e o elemento neutro.
//
//   Use case 1 - EDUCACAO: maior nota entre dois alunos         (MAX)
//   Use case 2 - FINANCAS: soma de vendas num intervalo          (SOMA)
//   Use case 3 - SAUDE:    menor saturacao de oxigenio no turno  (MIN)
//
// Como rodar:
//   rustc simple-operations.rs && ./simple-operations
//
// Como rodar os testes:
//   rustc --test simple-operations.rs -o simple-operations-test && ./simple-operations-test


use std::cmp::{max, min};

// INF = infinity. Neutral for min: min(anything, INF) = anything.
const INF: i32 = i32::MAX;


// ==========================================================
// STRUCT SegmentTree<T>
// ==========================================================
//
// Generic structure: works with any clonable type T.
// All three use cases (MAX, SUM, MIN) share the same code —
// only `merge` and `neutral` change.
//
//   - merge:   function that combines two nodes (e.g. max, +, min)
//   - neutral: element that does not affect the merge (e.g. -1, 0, INF)
//
// Internal layout: 1-indexed vector of size 4*n.
//   node 1     = root (covers [0, n-1])
//   left child = 2 * node
//   right child= 2 * node + 1

struct SegmentTree<T: Clone> {
    data:    Vec<T>,           // original array
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
        let mut st = SegmentTree { data, tree, n, neutral, merge };
        if n > 0 {
            st.build(1, 0, n - 1);
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

    fn build(&mut self, node: usize, start: usize, end: usize) {
        if start == end {
            self.tree[node] = self.data[start].clone();
            return;
        }
        let mid = (start + end) / 2;
        self.build(2 * node,     start,   mid);
        self.build(2 * node + 1, mid + 1, end);
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
            self.tree[node]  = value.clone();
            self.data[start] = value;
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
// Merge functions for i32
// ==========================================================
//
// The key insight: the tree skeleton never changes.
// Swapping merge + neutral solves max, sum, min, gcd, xor, etc.

fn merge_max(a: &i32, b: &i32) -> i32 { max(*a, *b) }
fn merge_sum(a: &i32, b: &i32) -> i32 { a + b }
fn merge_min(a: &i32, b: &i32) -> i32 { min(*a, *b) }


// ==========================================================
// print_tree - prints the tree level by level (use case 1 only)
// ==========================================================
// Shows the tree from top to bottom, each value centered in its slot.
// Useful for seeing that each level summarises larger blocks of the array.
// (Assumes M is a power of 2 — our case with M = 8.)
fn print_tree(tree: &[i32], m: usize) {
    // for M = 8 gives 4 levels (0 to 3)
    let levels = (usize::BITS - (m as usize).leading_zeros()) as usize;
    let width  = m * 3;             // each leaf takes ~3 chars at the bottom

    println!("Tree by levels (each node = max of the segment it covers):");
    for level in 0..levels {
        let nodes = 1usize << level;          // 1, 2, 4, 8 nodes per level
        let slot  = width / nodes;            // width of each node in the line
        let mut line = String::from("  ");
        for i in 0..nodes {
            let value = tree[nodes + i];      // indices: 1 / 2,3 / 4..7 / 8..15
            let text  = value.to_string();
            let pad   = slot.saturating_sub(text.len());
            let left  = pad / 2;
            let right = pad - left;
            line.push_str(&" ".repeat(left));
            line.push_str(&text);
            line.push_str(&" ".repeat(right));
        }
        println!("{}", line);
    }
    println!();
}

fn main() {
    println!("==============================================");
}


// ==========================================================
// UNIT TESTS
// ==========================================================
//
// rustc --test simple-operations.rs -o simple-operations-test && ./simple-operations-test

#[cfg(test)]
mod tests {
    use super::*;

    // ----------------------------------------------------------
    // MAX (grades)
    // ----------------------------------------------------------

    fn make_max_tree() -> SegmentTree<i32> {
        SegmentTree::new(vec![7, 3, 9, 5, 8, 2, 6, 4], -1, merge_max)
    }

    #[test]
    fn max_full_range() {
        let st = make_max_tree();
        assert_eq!(st.query(0, st.len() - 1), 9);
    }

    #[test]
    fn max_students_3_to_6() {
        let st = make_max_tree();
        // indices 2..5 = values [9, 5, 8, 2] -> max = 9
        assert_eq!(st.query(2, 5), 9);
    }

    #[test]
    fn max_students_6_to_8() {
        let st = make_max_tree();
        // indices 5..7 = values [2, 6, 4] -> max = 6
        assert_eq!(st.query(5, 7), 6);
    }

    #[test]
    fn max_single_element() {
        let st = make_max_tree();
        assert_eq!(st.query(0, 0), 7);
        assert_eq!(st.query(2, 2), 9);
    }

    #[test]
    fn max_update_student5() {
        let mut st = make_max_tree();
        // student 5 (index 4) retook exam: grade 10
        st.update(4, 10);
        assert_eq!(st.query(2, 5), 10);
        assert_eq!(st.root(), 10); // new root
    }

    // ----------------------------------------------------------
    // SUM (sales)
    // ----------------------------------------------------------

    fn make_sum_tree() -> SegmentTree<i32> {
        SegmentTree::new(vec![4, 1, 3, 5, 2, 6, 1, 2], 0, merge_sum)
    }

    #[test]
    fn sum_full_range() {
        let st = make_sum_tree();
        // 4+1+3+5+2+6+1+2 = 24
        assert_eq!(st.query(0, st.len() - 1), 24);
    }

    #[test]
    fn sum_days_3_to_6() {
        let st = make_sum_tree();
        // indices 2..5 = values [3, 5, 2, 6] -> sum = 16
        assert_eq!(st.query(2, 5), 16);
    }

    #[test]
    fn sum_update_day4() {
        let mut st = make_sum_tree();
        // day 4 (index 3) corrected from 5 to 7
        st.update(3, 7);
        // [3, 7, 2, 6] -> sum = 18
        assert_eq!(st.query(2, 5), 18);
        // total: 24 - 5 + 7 = 26
        assert_eq!(st.root(), 26);
    }

    // ----------------------------------------------------------
    // MIN (spo2)
    // ----------------------------------------------------------

    fn make_min_tree() -> SegmentTree<i32> {
        SegmentTree::new(vec![98, 97, 92, 94, 96, 99, 95, 93], INF, merge_min)
    }

    #[test]
    fn min_full_day() {
        let st = make_min_tree();
        assert_eq!(st.query(0, st.len() - 1), 92);
    }

    #[test]
    fn min_10h_to_16h() {
        let st = make_min_tree();
        // indices 2..5 = values [92, 94, 96, 99] -> min = 92
        assert_eq!(st.query(2, 5), 92);
    }

    #[test]
    fn min_single_element() {
        let st = make_min_tree();
        assert_eq!(st.query(0, 0), 98);
        assert_eq!(st.query(2, 2), 92);
    }

    #[test]
    fn min_update_reading_10h() {
        let mut st = make_min_tree();
        // 10h reading (index 2) corrected from 92 to 90
        st.update(2, 90);
        assert_eq!(st.query(2, 5), 90);
        assert_eq!(st.root(), 90); // new global min
    }
}
