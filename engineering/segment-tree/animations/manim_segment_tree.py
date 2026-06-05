from manim import *

ARRAY = [7, 3, 9, 5, 8, 2, 6, 4]
N = len(ARRAY)

IDLE = WHITE
VISIT = YELLOW
INSIDE = GREEN
OUTSIDE = "#666666"
PARTIAL = ORANGE
UPDATE = BLUE

NEUTRO = -1

def compute_layout():
    leaf_xs = [-6 + i * (12 / (N - 1)) for i in range(N)]
    level_y = [2.5, 1.0, -0.5, -2.0]
    positions = {}
    ranges = {}

    def rec(no, inicio, fim, depth):
        ranges[no] = (inicio, fim)
        if inicio == fim:
            positions[no] = (leaf_xs[inicio], level_y[depth])
            return
        meio = (inicio + fim) 
        rec(2 * no, inicio, meio, depth + 1)
        rec(2 * no + 1, meio + 1, fim, depth + 1)
        x = (positions[2 * no][0] + positions[2 * no + 1][0]) / 2
        positions[no] = (x, level_y[depth])

    rec(1, 0, N - 1, 0)
    return positions, ranges

class TreeMobject:
    def __init__(self):
        self.positions, self.ranges = compute_layout()
        self.circles = {}
        self.labels = {}
        self.range_lbl = {}
        self.edges = {}
        self.tree = {}

        for no, (x, y) in self.positions.items():
            circle = Circle(radius=0.32, color=IDLE, stroke_width=2)
            circle.move_to([x, y, 0])
            circle.set_fill(BLACK, opacity=1)
            self.circles[no] = circle
            self.labels[no] = Text("", font_size=22).move_to([x, y, 0])
            lo, hi = self.ranges[no]
            rlbl = Text(f"[{lo},{hi}]", font_size=14, color=GREY_B)
            rlbl.next_to(circle, DOWN, buff=0.05)
            self.range_lbl[no] = rlbl
            self.tree[no] = NEUTRO

        for no in self.positions:
            if 2 * no in self.positions:
                self.edges[(no, 2 * no)] = self._line(no, 2 * no)
            if 2 * no + 1 in self.positions:
                self.edges[(no, 2 * no + 1)] = self._line(no, 2 * no + 1)

    def _line(self, a, b):
        xa, ya = self.positions[a]
        xb, yb = self.positions[b]
        return Line([xa, ya - 0.32, 0], [xb, yb + 0.32, 0],
                    stroke_width=1.5, color=GREY_B)

    def all_mobjects(self):
        return (list(self.edges.values())
                + list(self.circles.values())
                + list(self.range_lbl.values())
                + list(self.labels.values()))

    def set_value(self, no, val):
        self.tree[no] = val
        old = self.labels[no]
        new = Text(str(val), font_size=22).move_to(old.get_center())
        self.labels[no] = new
        return Transform(old, new)

    def flash(self, no, color):
        return self.circles[no].animate.set_stroke(color, width=5)

    def reset_stroke(self, no):
        return self.circles[no].animate.set_stroke(IDLE, width=2)

def make_array_row(values, highlight=None, color=VISIT):
    cells = VGroup()
    for i, v in enumerate(values):
        box = Square(side_length=0.6, stroke_width=2, color=IDLE)
        box.set_fill(BLACK, opacity=1)
        txt = Text(str(v), font_size=22).move_to(box.get_center())
        idx = Text(str(i), font_size=14, color=GREY_B).next_to(box, UP, buff=0.05)
        group = VGroup(box, txt, idx)
        if highlight and highlight[0] <= i <= highlight[1]:
            box.set_stroke(color, width=3)
        cells.add(group)
    cells.arrange(RIGHT, buff=0.1)
    cells.to_edge(UP, buff=0.3)
    return cells

class BuildScene(Scene):
    def construct(self):
        title = Text("Segment Tree - BUILD (max)", font_size=30).to_edge(UP, buff=0.1)
        arr_row = make_array_row(ARRAY)
        self.play(Write(title))
        self.play(FadeIn(arr_row))

        tree = TreeMobject()
        self.play(
            *[Create(e) for e in tree.edges.values()],
            *[Create(c) for c in tree.circles.values()],
            *[FadeIn(r) for r in tree.range_lbl.values()],
            run_time=1.2,
        )

        caption = Text("", font_size=22).to_edge(DOWN, buff=0.3)
        self.add(caption)

        def set_caption(msg):
            nonlocal caption
            new = Text(msg, font_size=22).to_edge(DOWN, buff=0.3)
            self.play(Transform(caption, new), run_time=0.2)

        def build(no, inicio, fim):
            if inicio == fim:
                set_caption(f"folha [{inicio},{fim}] = {ARRAY[inicio]}")
                self.play(tree.flash(no, INSIDE), run_time=0.25)
                self.add(tree.labels[no])
                self.play(tree.set_value(no, ARRAY[inicio]), run_time=0.35)
                self.play(tree.reset_stroke(no), run_time=0.15)
                return
            meio = (inicio + fim) 
            self.play(tree.flash(no, VISIT), run_time=0.2)
            build(2 * no, inicio, meio)
            build(2 * no + 1, meio + 1, fim)
            esq = tree.tree[2 * no]
            dir_ = tree.tree[2 * no + 1]
            v = max(esq, dir_)
            set_caption(f"[{inicio},{fim}]: max({esq}, {dir_}) = {v}")
            self.play(tree.flash(no, PARTIAL), run_time=0.25)
            self.add(tree.labels[no])
            self.play(tree.set_value(no, v), run_time=0.35)
            self.play(tree.reset_stroke(no), run_time=0.15)

        build(1, 0, N - 1)
        set_caption(f"arvore pronta. raiz = max geral = {tree.tree[1]}")
        self.wait(2)

def prebuild(tree_mob):
    def rec(no, inicio, fim):
        if inicio == fim:
            tree_mob.tree[no] = ARRAY[inicio]
            tree_mob.labels[no] = Text(str(ARRAY[inicio]), font_size=22)\
                .move_to(tree_mob.circles[no].get_center())
            return
        meio = (inicio + fim) 
        rec(2 * no, inicio, meio)
        rec(2 * no + 1, meio + 1, fim)
        v = max(tree_mob.tree[2 * no], tree_mob.tree[2 * no + 1])
        tree_mob.tree[no] = v
        tree_mob.labels[no] = Text(str(v), font_size=22)\
            .move_to(tree_mob.circles[no].get_center())
    rec(1, 0, N - 1)

class QueryScene(Scene):
    def construct(self):
        L, R = 2, 5
        title = Text(f"Segment Tree - QUERY max em [{L},{R}]", font_size=30)\
            .to_edge(UP, buff=0.1)
        arr_row = make_array_row(ARRAY, highlight=(L, R), color=INSIDE)
        self.play(Write(title), FadeIn(arr_row))

        tree = TreeMobject()
        prebuild(tree)
        self.play(
            *[Create(e) for e in tree.edges.values()],
            *[Create(c) for c in tree.circles.values()],
            *[FadeIn(r) for r in tree.range_lbl.values()],
            *[FadeIn(l) for l in tree.labels.values()],
            run_time=1.2,
        )

        caption = Text("", font_size=22).to_edge(DOWN, buff=0.3)
        self.add(caption)

        def set_caption(msg):
            new = Text(msg, font_size=22).to_edge(DOWN, buff=0.3)
            self.play(Transform(caption, new), run_time=0.2)

        def query(no, inicio, fim):
            self.play(tree.flash(no, VISIT), run_time=0.3)
            if R < inicio or fim < L:
                set_caption(f"[{inicio},{fim}] fora de [{L},{R}] -> neutro (-1)")
                self.play(tree.circles[no].animate.set_stroke(OUTSIDE, width=4),
                          run_time=0.3)
                return NEUTRO
            if L <= inicio and fim <= R:
                v = tree.tree[no]
                set_caption(f"[{inicio},{fim}] dentro de [{L},{R}] -> retorna {v}")
                self.play(tree.circles[no].animate.set_stroke(INSIDE, width=4),
                          run_time=0.3)
                return v
            set_caption(f"[{inicio},{fim}] parcial -> desce nos dois filhos")
            self.play(tree.circles[no].animate.set_stroke(PARTIAL, width=4),
                      run_time=0.3)
            meio = (inicio + fim) 
            esq = query(2 * no, inicio, meio)
            dir_ = query(2 * no + 1, meio + 1, fim)
            v = max(esq, dir_)
            set_caption(f"[{inicio},{fim}] combina: max({esq},{dir_}) = {v}")
            return v

        resp = query(1, 0, N - 1)
        set_caption(f"resposta da query [{L},{R}] = {resp}")
        self.wait(2.5)

class UpdateScene(Scene):
    def construct(self):
        POS, NEW = 4, 10
        title = Text(f"Segment Tree - UPDATE pos={POS} -> {NEW}", font_size=30).to_edge(UP, buff=0.1)
        arr_row = make_array_row(ARRAY, highlight=(POS, POS), color=UPDATE)
        self.play(Write(title), FadeIn(arr_row))

        tree = TreeMobject()
        prebuild(tree)
        self.play(
            *[Create(e) for e in tree.edges.values()],
            *[Create(c) for c in tree.circles.values()],
            *[FadeIn(r) for r in tree.range_lbl.values()],
            *[FadeIn(l) for l in tree.labels.values()],
            run_time=1.2,
        )

        caption = Text("", font_size=22).to_edge(DOWN, buff=0.3)
        self.add(caption)

        def set_caption(msg):
            new = Text(msg, font_size=22).to_edge(DOWN, buff=0.3)
            self.play(Transform(caption, new), run_time=0.2)

        def update(no, inicio, fim):
            self.play(tree.flash(no, UPDATE), run_time=0.3)
            if inicio == fim:
                set_caption(f"folha pos={POS}: {tree.tree[no]} -> {NEW}")
                self.add(tree.labels[no])
                self.play(tree.set_value(no, NEW), run_time=0.5)
                return
            meio = (inicio + fim) 
            if POS <= meio:
                set_caption(f"[{inicio},{fim}]: POS={POS} <= meio={meio}, desce esquerda")
                update(2 * no, inicio, meio)
            else:
                set_caption(f"[{inicio},{fim}]: POS={POS} > meio={meio}, desce direita")
                update(2 * no + 1, meio + 1, fim)
            esq = tree.tree[2 * no]
            dir_ = tree.tree[2 * no + 1]
            v = max(esq, dir_)
            old = tree.tree[no]
            set_caption(f"[{inicio},{fim}]: recalcula max({esq},{dir_}) = {v} (antes: {old})")
            self.add(tree.labels[no])
            self.play(tree.set_value(no, v), run_time=0.4)
            self.play(tree.circles[no].animate.set_stroke(IDLE, width=2), run_time=0.15)

        update(1, 0, N - 1)
        set_caption(f"update ok. nova raiz (max geral) = {tree.tree[1]}")
        self.wait(2.5)

