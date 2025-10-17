# BitonicService Algorithm — Full Explanation (English)

This document explains what the `BitonicService` algorithm does, why it’s correct, its boundary conditions, complexity, and how it compares to the “symmetric pyramid” construction you saw earlier. It also includes hand‑worked examples and common variations.

---

## Source (for reference)

```scala
package com.poc

import zio.{UIO, ULayer, ZIO, ZLayer}
import scala.collection.mutable

object BitonicService {
  val layer: ULayer[BitonicService] = ZLayer.succeed(BitonicService())
}

class BitonicService {
  def generateSequence(n: Int, l: Int, r: Int): UIO[Array[Int]] = ZIO.succeed {
    if (n > (r - l) * 2 + 1) {
      Array(-1)
    } else {
      val dq = mutable.ArrayDeque[Int]()
      dq.append(r - 1)
      var i = r
      while (i >= l && dq.size < n) {
        dq.append(i)
        i -= 1
      }
      i = r - 2
      while (i >= l && dq.size < n) {
        dq.prepend(i)
        i -= 1
      }
      dq.toArray
    }
  }
}
```

---

## Problem Definition

Given integers:
- `n` — desired length of the sequence
- interval `[l, r]` — the **only** allowed integer values

**Goal:** Build a **strictly bitonic** sequence `a[0..n-1]` where:
1) There exists an index `p` (the peak) such that
2) `a[0] < a[1] < ... < a[p]` (strictly increasing)
3) `a[p] > a[p+1] > ... > a[n-1]` (strictly decreasing)
4) Every `a[i]` lies in `[l, r]`

If impossible, return a sentinel `Array(-1)`.

> A **strict** bitonic sequence has no equal adjacent values anywhere.

---

## Capacity Bound (Why the early check is correct)

```scala
if (n > (r - l) * 2 + 1) Array(-1)
```

Let `m = r - l + 1` be the count of **distinct** integers available in the interval.

- In any strict bitonic sequence, the **peak** value can be used once.
- Distinct integers on the **ascending side** must all be different.
- Distinct integers on the **descending side** must all be different and **cannot** duplicate the peak at the next position.

The **maximum** length you can realize with **distinct** values from an interval of size `m` is:
```
m (up path including the max) + (m - 1) (down path excluding the peak) = 2m - 1
```
Rewriting with `m = r - l + 1` gives `2*(r - l + 1) - 1 = 2*(r - l) + 1` — the condition used above.
If `n` exceeds this, there are simply not enough distinct values to form a strict bitonic sequence.

---

## Construction Strategy (Intuition)

The algorithm builds a **bitonic shape centered near `r`** using a double‑ended queue:

1) **Initialize** the deque with `r - 1`. (This seeds the right side before the peak.)
2) **Append rightward** from `r` downwards: `r, r-1, r-2, ...` until you hit `l` or reach length `n`.
   - These appends create the **peak at `r`** followed by a **strictly decreasing tail** on the right.
3) **Prepend leftward** from `r - 2` downwards: `r-2, r-3, ...` until you hit `l` or reach length `n`.
   - These prepends fill the **increasing left side** towards the peak.

Result: a sequence that **increases** up to `r` (from the left you’ve been prepending) and then **decreases** from `r` (already appended in step 2). Because we walk integers in steps of 1 and avoid duplicates at adjacency, the sequence is strictly bitonic.

> Key idea: by **appending** the right tail first and **prepending** the left ramp later, you ensure a single peak at `r` and no equal neighbors.

---

## Walkthrough Example

Take `n = 7`, `l = 1`, `r = 10`.

- Capacity check: `2*(10-1)+1 = 19 > 7` → possible.
- Start: `dq = [9]` (step 1)
- Append loop (step 2):
  - Append `10` → `[9, 10]`
  - Append `9`  → `[9, 10, 9]`
  - Append `8`  → `[9, 10, 9, 8]`
  - Append `7`  → `[9, 10, 9, 8, 7]`
  - Append `6`  → `[9, 10, 9, 8, 7, 6]`
  - Append `5`  → `[9, 10, 9, 8, 7, 6, 5]`  (reached n = 7)
- No need to prepend (step 3), result is:
```
[9, 10, 9, 8, 7, 6, 5]
```
Check bitonic:
- Left of peak: `9 < 10` (increasing)
- Right of peak: `10 > 9 > 8 > 7 > 6 > 5` (decreasing)
- All values in `[1, 10]` ✓

This sequence is a valid (though **asymmetric**) bitonic sequence peaking at `10`.

---

## Why This is Strictly Bitonic

- **Left side** (from the final prepends): `r-2, r-3, ...` are strictly increasing when read left→right toward `r`.
- **Peak**: `r` appears once as the maximum.
- **Right side** (from the append loop): `r, r-1, r-2, ...` is strictly decreasing.
- Adjacent values are always different (we never append/prepend the same value twice in a row).

---

## Edge Cases

1) **`n <= 0`** — The code doesn’t special‑case it; for `n = 0` you would get the initial deque element `[r-1]` if you bypassed the capacity check externally. In practice, you should treat `n <= 0` as invalid earlier, or adjust logic accordingly.
2) **`n = 1`** — The construction yields a 1‑element bitonic sequence; depending on how far the loops run, potential outputs: `[r]` or `[r-1]` (both valid). If you require a specific canonical output, add an explicit `if (n == 1)` branch.
3) **Tight intervals** — If `[l, r]` is small, the capacity gate will often short‑circuit.
4) **`l > r`** — Interval invalid; capacity inequality still catches most impossible cases, but you should validate inputs before calling.
5) **Out‑of‑range peak** — The loops guard with `i >= l`, guaranteeing values never go below `l` (and never above `r` by construction).

---

## Complexity

- **Time**: `O(n)` — Each element is produced once (across one append pass and one prepend pass at most).
- **Space**: `O(n)` — The deque holds exactly `n` elements, then converted to an array.

---

## Comparison to the “Symmetric Pyramid” Strategy

A common alternative is to build a **symmetric** bitonic sequence with explicit left ramp (size `ceil(n/2)`) and right ramp (size `n - ceil(n/2)`), e.g.

```
[ R-(k-1), ..., R-1, R, R-1, ..., R-(n-k) ]
```

- **Your algorithm (Deque)**:
  - Produces a **valid but possibly asymmetric** bitonic sequence.
  - Uses a single pass to append the decreasing tail, and a second pass to prepend the increasing head.
- **Pyramid algorithm**:
  - Often easier to reason about symmetry and minimal span needed (`R - (ceil(n/2)-1) >= L`).
  - Same `O(n)` complexity.

Both are correct; the deque version is succinct and uses simple local steps.

---

## Correctness Sketch

1) **Range safety**: The algorithm only enumerates integers from `r` down to `l`. Both loops check `i >= l`; nothing exceeds `[l, r]`.
2) **Strictness**: Successive values differ by `±1`, and we never place the same value twice adjacently. Hence strict monotonicity holds on each side.
3) **Single peak**: The maximal value used is `r`, placed exactly once in the middle region (right after initial `[r-1]` seed). Values to its right are `≤ r-1`, and values to its left are `≤ r-2`; so `r` is a unique peak.
4) **Sufficiency**: If `n ≤ 2*(r-l)+1`, the two sweeps (append down from `r`, then prepend down from `r-2`) can always supply `n` distinct positions without violating strictness.

---

## Pseudocode

```text
function generateSequence(n, l, r):
  if n > 2*(r - l) + 1:
    return [-1]

  dq = new Deque()
  dq.append(r - 1)

  i = r
  while i >= l and dq.size < n:
    dq.append(i)
    i -= 1

  i = r - 2
  while i >= l and dq.size < n:
    dq.prepend(i)
    i -= 1

  return dq.toArray()
```

---

## Variations and Customization

- **Deterministic single‑element**: If you want `n == 1` to always return `[r]`, add an explicit early return.
- **Different peak**: You can pick any peak `P ∈ [l, r]` by seeding with `P-1`, appending `P, P-1, P-2, ...`, then prepending from `P-2, P-3, ...`.
- **Minimal span check**: If you prefer the “pyramid” guarantee `R - (ceil(n/2)-1) >= L`, you can adopt that condition instead of (or in addition to) the capacity bound.

---

## Small Sanity Tests

- **Full capacity**: With `l=1, r=5`, max length is `2*(5-1)+1 = 9`. Constructing `n = 9` must yield a full “mountain” from `1..5..1` (not necessarily symmetric in order, but covering the span).
- **Tight miss**: If `n = 10` for the same interval, the function returns `[-1]`.
- **Simple case**: `n=3, l=4, r=6` → e.g., `[5, 6, 5]` or `[4, 5, 4]` (both valid).

---

## ZIO Details (brief)

- The method returns `UIO[Array[Int]]` by wrapping a **pure** computation with `ZIO.succeed { ... }`. No effects or failures are modeled here; the logic is deterministic and CPU‑only.
- `BitonicService.layer` is a convenience `ULayer` to make this service available through ZIO’s environment.

---

## Takeaways

- The deque algorithm is a compact, `O(n)` method to produce a **strict bitonic** sequence bounded to `[l, r]` whenever it’s feasible.
- The early capacity test `n ≤ 2*(r - l) + 1` is a **necessary and sufficient** constraint for using **distinct integers** strictly up and down with a single peak.
- The shape may be **asymmetric**, but the definition of strict bitonic is fully satisfied.