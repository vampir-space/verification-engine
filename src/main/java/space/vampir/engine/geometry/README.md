# GeometrySolver

The `GeometrySolver` estimates a vehicle’s **2D position** and **orientation** using three types of information:

---

### **1. Odometry prior**

An approximate prior pose $(x_o, y_o)$ with orientation angle $\alpha_o$, represented as a unit vector:

$$
(\cos \alpha_o, \sin \alpha_o)
$$

and an associated confidence weight $c \in [0, 1]$.

---

### **2. Location detections**

Direct observations of the vehicle’s position at points

$$
(x_i, y_i)
$$

with uncertainty radius $r_i$.

These act as **point constraints** that pull the estimated position toward the detected locations.

---

### **3. YOLO detections**

Each **YOLO detection** comes from a visual model that identifies a known **landmark** in the camera view.  
The landmark’s **world position** is known:

$$
(l_{x,i}, l_{y,i})
$$

and the **bearing angle** $\beta_i$ (in **degrees**) is measured **relative to the car’s current forward direction**.

If the car’s true facing direction is the unit vector

$$
d = (\cos \alpha, \sin \alpha),
$$

then after rotating this direction by $\beta_i$ degrees, the resulting vector

$$
d_i = R(\beta_i) \, d
$$

points **toward** the detected landmark.

From the **landmark’s perspective**, the car must lie somewhere along a **ray** that starts at the landmark and points in the **opposite direction** of that rotated vector:

$$
r_i = -R(\beta_i) \, d
$$

Each YOLO detection therefore provides a **ray constraint**, indicating that the car should lie somewhere along this ray extending outward from the detected landmark.

---

## **Estimation Procedure**

The solver estimates both the position $(x, y)$ and orientation $\alpha$ that best satisfy all constraints using an **iterative least-squares refinement**:

1. **Fix the orientation** $\alpha$ and solve for position $(x, y)$ based on geometric intersections of the YOLO-derived rays and point detections.  
2. **Fix the position** $(x, y)$ and update the orientation $\alpha$ using the bearings toward all visible landmarks (from YOLO detections).  
3. Repeat until the pose $(x, y, \alpha)$ converges.

---

This alternating optimization produces a consistent estimate of the car’s position and facing direction by combining **landmark geometry** (from YOLO detections), **point measurements**, and **odometry priors** into a unified geometric solution.


---

## 1. Position Estimation

### Goal

We want to find the position `(x, y)` that minimizes how far we are from all the rays and points we “should” be consistent with.

Each ray can be thought of as saying:

> “You should be somewhere *along this line* starting from `(px, py)` and going in direction `(vx, vy)`.”

Each point says:

> “You should be *close to this point*.”

Odometry gives us an approximate prior position `(xo, yo)` with a certain weight `c` that says how much to trust it.

---

### Weighted Least Squares Setup

We minimize a weighted sum of squared distances:

```
E(x, y) = (1 - c) * [sum of ray and point distances²] + c * [distance to odometry²]
```

* `c ∈ [0, 1]` controls how strongly we trust the odometry.
* `(1 - c)` scales how much we trust the external detections.

This is a **least squares** problem that can be written in matrix form:

```
M * [x, y]^T = b
```

where:

* `M` is a 2×2 system derived from all active constraints,
* `b` is a 2×1 vector pulling towards the measured data.

Solving gives:

```
[x, y] = M⁻¹ * b
```

The solver uses an **active-set** approach:

* Rays can only extend forward (they are *half-lines*).
* If the projection of `(x, y)` onto a ray would be *behind* it, that ray is treated like a point instead.
* The algorithm iterates until the active/inactive rays stop changing.

---

## 2. Orientation Estimation

### Intuition

Once we know where we are, we can estimate which direction the robot is facing.

Each landmark ray not only tells us *where* we are (geometry), but also *what angle* the robot would have had to face to see that landmark.

For a ray at direction `(vx, vy)`:

* Its **bearing angle** is simply `β = atan2(vy, vx)`.

If the robot is looking *toward* the landmark, its **facing direction** should be roughly `β + π` (180° rotated).

---

### Averaging Directions

We form **unit vectors** representing each estimated facing direction and average them together.

If `n` rays give facing directions `(fx_i, fy_i)`:

```
F = sum_i (fx_i, fy_i)
```

We then blend this with the odometry facing direction `(f_odo_x, f_odo_y)`:

```
F_total = (1 - c) * F + c * (f_odo_x, f_odo_y)
```

Normalize it to get the unit vector of the estimated heading:

```
(fx, fy) = F_total / ||F_total||
θ = atan2(fy, fx)
```

Here `θ` is the final estimated orientation.

---

## 3. Confidence Measures

### 3.1 Position Confidence — The Ellipse

If the rays and points intersect cleanly, we’re confident in our position.
If they’re almost parallel or noisy, we’re less sure.

Mathematically, we get this from the same matrix `M` we solved earlier:

```
Covariance ≈ M⁻¹
```

This matrix defines an **ellipse** that describes how uncertain we are in different directions:

* The **shape** comes from how the rays intersect (wide angles → round ellipse, shallow angles → flat ellipse).
* The **size** scales with how much residual error there was in the fit.

We draw it as a 1σ ellipse centered at the solution `(x, y)`.

---

### 3.2 Orientation Confidence — The Cone

We also measure how strongly the rays “agree” on the facing direction.

Before normalization, the length of the summed vector `F_total` gives a natural confidence:

```
r = ||F_total||
```

* If all bearings agree → `r ≈ 1.0` → narrow cone.
* If bearings conflict → `r` becomes small → wide cone.

We can visualize this as a **cone** (or sector) centered on the estimated heading `θ`, with half-angle proportional to `(1 - r)`.

For example:

```
half_angle = (1 - r) * 45°  // rough intuitive scaling
```

---

## 4. Visualization Summary

| Element               | Color              | Meaning                               |
| --------------------- | ------------------ | ------------------------------------- |
| **Rays**              | Green              | Landmarks with viewing direction      |
| **Points**            | Blue               | Fixed spatial constraints             |
| **Odometry point**    | Red                | Prior position from dead reckoning    |
| **Solution**          | Black              | Estimated position                    |
| **Odometry heading**  | Red arrow          | Original odometry facing direction    |
| **Estimated heading** | Gray/Black arrow   | Computed best-fit orientation         |
| **Position ellipse**  | Light gray outline | Confidence area in position           |
| **Heading cone**      | Faint gray wedge   | Confidence spread in facing direction |

---

## 5. Iterative Refinement

In a full system, we can alternate between updating position and orientation:

1. **Start** with an initial guess of position and orientation.
2. **Fix orientation**, solve for position (the least-squares step).
3. **Fix position**, update orientation (average bearings).
4. Repeat until both stabilize.

This creates a coupled, iterative solution that converges to a consistent `(x, y, θ)`.

---

## 6. Intuition Recap

* Position = where all geometric constraints best intersect.
* Orientation = average of all “where I must be facing to see that landmark.”
* The weights `c` let us smoothly combine odometry and perception.
* Ellipse and cone show how confident we are in each estimate.

---

## References

This approach is conceptually similar to pose estimation in robotics and SLAM systems using **least squares** and **bearing-only localization** (see e.g. Thrun, Burgard, Fox *Probabilistic Robotics*), but simplified for implementation in a small, efficient solver.
