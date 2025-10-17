# GeometrySolver

The `GeometrySolver` estimates a vehicle's **2D position** and **orientation** using three types of information:

---

### **1. Odometry prior**

An approximate prior pose $(x_o, y_o)$ with orientation angle $\alpha_o$, represented as a unit vector:

$$
(\cos \alpha_o, \sin \alpha_o)
$$

and an associated confidence weight $c \in [0, 1]$.

---

### **2. Location detections**

Direct observations of the vehicle's position at points

$$
(x_i, y_i)
$$

with an uncertainty radius $r_i$.

These act as **point constraints** that pull the estimated position toward the detected locations within a circular confidence region of radius $r_i$.

---

### **3. YOLO detections**

Each **YOLO detection** comes from a visual model that identifies a known **landmark** in the camera view.  
The landmark's **world position** is known:

$$
(l_{x,i}, l_{y,i})
$$

and the **bearing angle** $\beta_i$ (in **degrees**) is measured **relative to the car's current forward direction**.

If the car's true facing direction is the unit vector

$$
d = (\cos \alpha, \sin \alpha),
$$

then after rotating this direction by $\beta_i$ degrees, the resulting vector

$$
d_i = R(\beta_i) \, d
$$

points **toward** the detected landmark.

From the **landmark's perspective**, the car must lie somewhere along a **ray** that starts at the landmark and points in the **opposite direction** of that rotated vector:

$$
r_i = -R(\beta_i) \, d
$$

Each YOLO detection therefore provides a **ray constraint**, indicating that the car should lie somewhere along this ray extending outward from the detected landmark.

To model visual uncertainty, each ray is also assigned an **angular confidence cone** with half-angle $\sigma_{\beta_i}$ (in degrees).  
This cone represents the possible deviation of the measured bearing $\beta_i$ due to perception noise or pixel quantization:

$$
\beta_i \pm \sigma_{\beta_i}
$$

A smaller $\sigma_{\beta_i}$ implies a sharper, more confident detection.

---

## **Constraint Weighting from Uncertainties**

To incorporate the uncertainty measures into the least-squares estimation, we derive scalar weights for each constraint:

### **Location Detection Weights**

For each location detection with uncertainty radius $r_i$, the weight is inversely proportional to the variance (radius squared):

$
w_{\text{loc},i} = \frac{1}{r_i^2 + \varepsilon}
$

where $\varepsilon$ is a small regularization constant (e.g., $\varepsilon = 0.01$ m²) to handle perfect measurements where $r_i = 0$. Smaller uncertainty radius → larger weight → stronger constraint.

### **YOLO Detection Weights**

For each YOLO detection with angular uncertainty $\sigma_{\beta_i}$ (in degrees), we derive a weight based on the angular precision. Converting to radians and using inverse variance weighting:

$
w_{\text{yolo},i} = \frac{1}{(\sigma_{\beta_i} \cdot \pi/180)^2 + \varepsilon}
$

where $\varepsilon$ is a small regularization constant (e.g., $\varepsilon = (0.1 \cdot \pi/180)^2 \approx 3 \times 10^{-6}$) to handle the case where $\sigma_{\beta_i} = 0$ (perfect measurement). This gives a very large but finite weight for highly confident detections.

---

## **Iterative Estimation Procedure**

The solver alternates between position and orientation estimation until convergence:

### **Initialization**

Start with odometry prior: $(x, y) = (x_o, y_o)$ and $\alpha = \alpha_o$.

### **Iteration Loop** (repeat until convergence)

#### **Step 1: Position Estimation (fix orientation $\alpha$)**

Given current orientation $\alpha = (\cos \alpha, \sin \alpha)$, compute ray directions for all YOLO detections:

For each landmark at $(l_{x,i}, l_{y,i})$ with bearing $\beta_i$:

$$
r_i = -R(\beta_i) \, d
$$

This ray direction is now fixed for this iteration.

**Weighted Least Squares for Position:**

Minimize the weighted sum:

$$
E(x, y) = \sum_{\text{locations}} w_{\text{loc},i} \cdot \left\| (x, y) - (x_i, y_i) \right\|^2 
+ \sum_{\text{YOLO}} w_{\text{yolo},i} \cdot \text{dist}_{\text{ray}}^2(x, y; l_i, r_i)
+ w_{\text{odo}} \cdot \left\| (x, y) - (x_o, y_o) \right\|^2
$$

where:
- $\text{dist}_{\text{ray}}(x, y; l_i, r_i)$ is the perpendicular distance from point $(x, y)$ to the ray starting at $l_i$ in direction $r_i$
- $w_{\text{odo}} = c$ is the odometry weight

This can be formulated as a linear system:

$$
M \begin{bmatrix} x \\ y \end{bmatrix} = b
$$

where $M$ is a $2 \times 2$ weighted normal equations matrix and $b$ is the weighted right-hand side.

**Active Set Handling:**
- Rays are half-lines (one-sided constraints)
- If the projection of $(x, y)$ onto ray $i$ falls behind the landmark, treat it as a point constraint at $(l_{x,i}, l_{y,i})$ instead
- Iterate until the active/inactive status stabilizes

Solve: $(x, y) = M^{-1} b$

#### **Step 2: Orientation Estimation (fix position $(x, y)$)**

Given current position $(x, y)$, each YOLO landmark provides a bearing constraint.

For each landmark at $(l_{x,i}, l_{y,i})$, the vector from the vehicle to the landmark is:

$$
v_i = (l_{x,i} - x, l_{y,i} - y)
$$

The angle from vehicle to landmark is:

$$
\theta_i = \operatorname{atan2}(l_{y,i} - y, l_{x,i} - x)
$$

Given that the bearing measurement was $\beta_i$ (relative to vehicle heading), the vehicle heading that would produce this bearing is:

$$
\alpha_i = \theta_i - \beta_i \cdot \pi/180
$$

**Weighted Circular Mean:**

Each YOLO detection provides an orientation estimate $\alpha_i$ with weight $w_{\text{yolo},i}$.

Convert to unit vectors and compute weighted sum:

$$
S_x = \sum_{\text{YOLO}} w_{\text{yolo},i} \cdot \cos(\alpha_i) + w_{\text{odo}} \cdot \cos(\alpha_o)
$$

$$
S_y = \sum_{\text{YOLO}} w_{\text{yolo},i} \cdot \sin(\alpha_i) + w_{\text{odo}} \cdot \sin(\alpha_o)
$$

The updated orientation is:

$$
\alpha = \operatorname{atan2}(S_y, S_x)
$$

#### **Convergence Check**

Stop when both position and orientation changes fall below thresholds:

$
\left\| (x_{\text{new}}, y_{\text{new}}) - (x_{\text{old}}, y_{\text{old}}) \right\| < \varepsilon_{\text{pos}}
$

$
|\alpha_{\text{new}} - \alpha_{\text{old}}| < \varepsilon_{\text{angle}}
$

Typical values: $\varepsilon_{\text{pos}} = 0.01$ meters, $\varepsilon_{\text{angle}} = 0.1$ degrees.

---

## **Confidence Estimation**

### **Position Confidence Ellipse**

The covariance of the position estimate is approximated by:

$$
\Sigma_{\text{pos}} = M^{-1}
$$

where $M$ is the weighted normal equations matrix from the position step.

Eigenvalue decomposition of $\Sigma_{\text{pos}}$ gives:
- **Major/minor axes**: eigenvectors scaled by $\sqrt{\lambda_i}$
- **Ellipse orientation**: angle of largest eigenvector

This visualizes the $1\sigma$ confidence region.

### **Orientation Confidence**

The concentration of the weighted orientation estimates is measured by:

$$
r = \frac{\sqrt{S_x^2 + S_y^2}}{\sum_{\text{all}} w_i}
$$

This gives a value in $[0, 1]$ where:
- $r \approx 1$: all bearings agree (high confidence)
- $r \approx 0$: bearings conflict (low confidence)

Visualize as a cone with half-angle:

$$
\sigma_{\alpha} = k \cdot (1 - r) \cdot 180°
$$

where $k$ is a scaling factor (e.g., $k = 0.5$ gives max 90° half-angle).

---

## **Implementation Notes**

1. **Normalization**: Consider normalizing all weights to sum to 1 within each constraint type
2. **Robust estimation**: For outlier rejection, consider using Huber or Tukey weights
3. **Initialization**: Good initial guess from odometry significantly speeds convergence
4. **Degenerate cases**: 
   - Single ray: no position estimate possible
   - Parallel rays: position poorly constrained in one direction
   - No YOLO detections: rely purely on location detections and odometry

---

## **Summary**

The iterative procedure:
1. Converts uncertainty measures ($r_i$, $\sigma_{\beta_i}$) into scalar weights ($w_i$)
2. Alternates between weighted least-squares position and orientation estimation
3. Converges to a consistent pose $(x, y, \alpha)$ that balances all constraints
4. Provides confidence measures through covariance analysis

This creates a unified geometric solution combining landmark geometry, point measurements, and odometry priors with proper uncertainty handling.