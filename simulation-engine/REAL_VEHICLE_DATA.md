# Initial real-vehicle validation data

These development fixtures support the catalog and baseline validation. They are not
production-certified models. Values are classified so an estimate cannot be
mistaken for a verified specification.

## October 2026 validation and intake notes

- Eight vehicles are now seeded development catalog records. “Simulation-ready”
  means the required inputs pass structural checks, not that performance is independently validated.
- `GET /api/v1/vehicles/{id}/readiness` exposes blockers, assumptions and existing
  database source/confidence records. A source attached to a trim does not verify every field.
- The Golf's 40–120 mph test is a regression guard, not measured evidence of accuracy.
  Golf market/rating consistency and DSG effective-ratio attribution still need review.
- Benchmark reports distinguish instantaneous finish speed from published trap speed;
  that proxy is excluded from aggregate timing error. Added rollout time is an approximation
  unless the source reports the actual omitted interval for that run.
- New automated tests cover 128 combinations of vehicle pairs, 0–70 mph starts,
  both supported distances and all four surfaces, plus timestep convergence and determinism.
  These establish numerical reliability, not real-world tire or transmission accuracy.

Additional catalog configurations seeded by V11 (detailed evidence below):

| Vehicle | Configuration | Remaining accuracy work |
| --- | --- | --- |
| 2021 BMW M3 Competition | US RWD sedan | Matched test mass, measured curves and rolling benchmarks |
| 2019 Mercedes-AMG C63 S | Sedan assumed | Rollout convention, launch/shift characterization |
| 2019 Chevrolet Corvette C7 Z06 | Automatic coupe, standard aero assumed | US test mass and matched standard-aero benchmarks |
| 2022 BMW M8 Competition | Coupe, US power/mass references | Matched 2022 weigh-in and performance benchmarks |

Intake sequence: identify exact variant → attach sources/licensing notes → normalize SI units
→ record estimated inputs explicitly → run structural checks → seed a development fixture
with a new migration → validate standing and rolling benchmarks under matched conditions.
Missing comparison data stays missing;
peak horsepower alone is not sufficient to enable a vehicle.

## 2024 Ford Mustang GT Performance Package, six-speed manual

Verified/tested values:

- 3,947 lb tested curb weight
- RWD and six-speed manual transmission
- 486 hp at 7,250 rpm with active exhaust
- 418 lb-ft at 4,900 rpm
- 3.73 final drive
- 255/40R19 front and 275/40R19 rear tires
- 107.0-inch wheelbase
- Tested 0-60 mph: 4.2 seconds
- Tested quarter mile: 12.5 seconds at 114 mph
- Test figures omit a 0.3-second one-foot rollout

Sources:

- [Car and Driver instrumented test](https://www.caranddriver.com/reviews/a44892132/2024-ford-mustang-gt-test/)
- [Ford technical specifications](https://media.ford.com/content/dam/fordmedia/Europe/en/2024/01/NewMustang/2024_Ford_Mustang_technical_specification_EU.pdf)

Estimated or assumed values:

- Intermediate torque-curve points are estimates constrained by the verified
  torque peak and the torque implied by peak horsepower.
- 55% front static weight distribution is an initial estimate.
- 0.55 m center-of-gravity height is an initial estimate.
- 0.88 drivetrain efficiency is an initial estimate.
- 0.20-second manual shift duration is an initial estimate.
- 3,500-rpm launch, 0.45-second clutch engagement, and 55% initial torque
  transfer are initial deterministic launch assumptions.
- 0.377 drag coefficient and 2.20 m² frontal area require better primary-source
  confirmation.
- Tire friction and rolling resistance are initial calibration assumptions.

## 2020 Chevrolet Corvette Stingray Z51, eight-speed DCT

Verified/tested values:

- 3,647 lb tested curb weight
- Mid-engine RWD layout and eight-speed dual-clutch transmission
- 495 hp at 6,450 rpm
- 470 lb-ft at 5,150 rpm
- 245/35ZR19 front and 305/30ZR20 rear tires
- 107.2-inch wheelbase
- Tested 0-60 mph: 2.8 seconds
- Tested quarter mile: 11.2 seconds at 122 mph
- Test figures omit a 0.2-second one-foot rollout
- 2.075 m² frontal area and 0.322 Z51 drag coefficient
- Published DCT ratios and 5.17 final drive

Sources:

- [Car and Driver instrumented test](https://www.caranddriver.com/chevrolet/corvette-2024)
- [Chevrolet 2024 Stingray product sheet](https://media.chevrolet.com/content/dam/Media/images/US/Vehicles/Chevrolet/Cars/Corvette/2024/2024-Chevrolet-Corvette-Stingray.pdf)
- [Chevrolet technical specifications](https://media.chevrolet.com/content/dam/Media/documents/INTL/chevrolet/tech-data/corvette-stingray/CHEVROLET%20CORVETTE%20STINGRAY_Tech%20Specs_IT.pdf)
- [SAE/TREMEC C8 engineering report](https://tremec.com/wp-content/uploads/2023/05/Engineering.the_.C8.Corvette_SAE.Special.Report.pdf)

Estimated or assumed values:

- Intermediate torque-curve points are estimates constrained by the verified
  torque peak and the torque implied by peak horsepower.
- 40% front static weight distribution is an initial estimate.
- 0.48 m center-of-gravity height is an initial estimate.
- 0.90 drivetrain efficiency is an initial estimate.
- 0.08-second DCT shift duration is an initial estimate.
- 3,500-rpm launch-control target, 0.35-second clutch engagement, and 55%
  initial torque transfer are initial deterministic launch assumptions.
- Tire friction and rolling resistance are initial calibration assumptions.

## Benchmark caveat

Instrumented magazine results use a one-foot rollout, while the simulator starts
its clock at initial movement. The published numbers are intentionally retained
as reported for this first baseline. A later validation revision should compare
both rollout-adjusted and non-rollout times explicitly.

## 2023 Honda Civic Type R, six-speed manual

Verified/tested values:

- FWD, 315 hp at 6,500 rpm, and 310 lb-ft from 2,600-4,000 rpm
- 3,183 lb tested curb weight and 62/38 static weight distribution
- Six published gear ratios and 3.842 final drive
- 265/30ZR19 tires and 107.7-inch wheelbase
- Tested 0-60 mph: 4.9 seconds
- Tested quarter mile: 13.5 seconds at 106 mph
- Test figures omit a 0.3-second one-foot rollout

Sources:

- [Honda specifications brochure](https://d31sro4iz4ob5n.cloudfront.net/upload/car/civic-type-r-2023/brochure/civic-type-r-2023-lhd-brochure_en.pdf)
- [Car and Driver instrumented test](https://www.caranddriver.com/reviews/a41952459/2023-honda-civic-type-r-by-the-numbers/)

Estimated or assumed values:

- Intermediate torque-curve points, center-of-gravity height, drivetrain
  efficiency, shift duration, launch behavior, aerodynamic values, tire
  friction, and rolling resistance remain initial engineering estimates.

## 2022 Volkswagen Golf R Euro-spec, seven-speed DSG

Verified/tested values:

- AWD, 315 hp at 5,900 rpm, and 310 lb-ft at 1,900 rpm
- 3,360 lb tested curb weight
- Seven published DSG ratios; 4.47 and 3.30 transmission final drives are
  folded into their corresponding gear ratios in the fixture
- 235/35R19 tires and 103.5-inch wheelbase
- Tested 0-60 mph: 3.9 seconds
- Tested quarter mile: 12.5 seconds at 111 mph
- Test figures omit a 0.2-second one-foot rollout

Sources:

- [Volkswagen technical specifications](https://downloads.regulations.gov/NHTSA-2023-0022-0074/attachment_2.pdf)
- [Car and Driver instrumented test](https://www.caranddriver.com/reviews/a37200521/2022-volkswagen-golf-r-us-drive/)

Estimated or assumed values:

- Intermediate torque-curve points, static weight distribution,
  center-of-gravity height, drivetrain efficiency, shift duration, launch
  behavior, aerodynamic values, tire friction, and rolling resistance remain
  initial engineering estimates.
- The tested vehicle was Euro-spec; this fixture intentionally follows its
  published 310 lb-ft output rather than the U.S. DSG rating of 295 lb-ft.

## Four additional stock fixtures — migration V11

These cars are **structurally race-ready, not independently accuracy-certified**.
They use the same deterministic engine as the original catalog. No elapsed time,
winner or acceleration result is hardcoded into the simulation. Comparison figures
are separate reference data. The new fixtures have database integration coverage
at 0/40/70 mph over 1/8 and 1/4 mile; this establishes functionality, not real-world
validation. No existing vehicle's physics inputs were changed in V11.

### 2021 BMW M3 Competition RWD sedan

- Factory inputs: 503 hp at 6250 rpm, 479 lb-ft, 3890 lb, 53.1% front weight,
  112.5-inch wheelbase, standard 18/19-inch tires, eight ratios and 3.15 final drive.
- MSRP $72,800 excludes destination; EPA 16/23/19 mpg.
- [BMW USA specifications](https://www.press.bmwgroup.com/usa/article/detail/T0317577EN_US/the-new-2021-bmw-m3-sedan-and-m4-coupe?language=en_US).
- [C&D reference test](https://www.caranddriver.com/reviews/a37286079/2021-bmw-m3-competition-by-the-numbers/):
  3.5 s 0–60, 11.6 s quarter at 124 mph, omitting 0.2 s rollout.
  That test car weighed 3820 lb, unlike this factory-weight fixture. Do not
  present the comparison as a matched-condition calibration.
- Estimated Cd 0.34, frontal area 2.25 m², CG height 0.52 m, efficiency 0.89.

### 2019 Mercedes-AMG C63 S sedan

- Sedan assumed because body style was not specified. MCT uses a wet start clutch,
  not a torque converter or dual-clutch gearset. Added WET_CLUTCH_AUTOMATIC to
  represent that distinction; it still uses the engine's simplified shift model.
- [Mercedes-Benz USA specifications](https://media.mbusa.com/news/2019-mercedes-amg-c-63-and-c-63-s-sedan-specifications):
  503 hp, 516 lb-ft, nine ratios, 2.82 final drive, 111.8-inch wheelbase.
  MSRP $74,600 excludes destination; EPA 18/27/21 mpg.
- [C&D sedan test](https://www.caranddriver.com/reviews/a22174935/2019-mercedes-amg-c63-first-drive-review/):
  uses tested 3987 lb and 245/35ZR19 front / 265/35ZR19 rear tires;
  3.7 s 0–60 and 11.9 s quarter at 122 mph. Rollout unspecified, stored NULL.
  The factory page lists a different front tire aspect ratio; this fixture follows
  the tested configuration rather than silently combining those tire sizes.
- Estimated front fraction 0.54, CG 0.53 m, Cd 0.32, area 2.20 m², efficiency 0.89.

### 2019 Chevrolet Corvette Z06 automatic coupe, standard aero

- Assumed coupe and standard aero, not Z07. 650 hp/650 lb-ft LT4,
  8L90 ratios with 2.41 final drive, 50/50 distribution and 2710 mm wheelbase.
- [2019 Chevrolet European technical sheet](https://media.cadillac.com/content/dam/Media/documents/INTL/chevrolet/2019/vehicles/corvette-z06/Tech-Data-Chevrolet-Corvette-Z06.pdf)
  supplies the automatic's 1659 kg curb mass (NOT the 1734 kg running mass),
  gearing and standard Michelin Pilot Super Sport tire sizes.
- [Chevrolet US brochure, distributor-hosted copy](https://cdn.dealereprocess.org/cdn/brochures/chevrolet/2019-corvette.pdf)
  supplies US SAE output. European output is quoted in PS/kW; do not treat 659 PS
  as 659 SAE hp. This initial fixture combines US output and European curb mass;
  a matched US automatic weigh-in is still desirable.
- Estimated CG 0.46 m, Cd 0.36, area 2.05 m², efficiency 0.89.
- MSRP, fuel economy and matched 0–60/quarter reference left unavailable.
  Factory Z07 results must not be presented as standard-aero results. The European
  0–100 km/h figure is not a 0–60 mph figure.

### 2022 BMW M8 Competition coupe

- 617 hp at 6000 rpm, 553 lb-ft, AWD, eight ratios, 3.154 final drive,
  2827 mm wheelbase, 275/35ZR20 front and 285/35ZR20 rear.
- [2022 BMW global technical sheet](https://www.press.bmwgroup.com/global/article/attachment/T0364657EN/519030):
  Cd 0.33 and frontal area 2.25 m², dimensions, tires and ratios.
- [BMW USA F92 powertrain reference](https://www.press.bmwgroup.com/usa/article/detail/T0296777EN_US/the-new-2020-bmw-m8-coupe-and-convertible)
  supplies US power ratings, redline and 4295 lb curb-mass reference. This is a
  2020 family reference carried into the 2022 fixture, not a 2022 weigh-in.
- [BMW 2022 model-year pricing](https://s3.amazonaws.com/bmwmedia.iconicweb.com/mediasite/attachments/2022_BMW_Model_Year_Update_Guide_v2.pdf):
  $130,000 before destination. Not the earlier $146,000 price.
- Estimated front fraction 0.54, CG 0.52 m, efficiency 0.86. Fuel economy and
  matched 2022 instrumented performance remain unavailable, not borrowed from 2020.

### Shared modeling assumptions and next validation work

All four use an estimated 0.10 s shift interruption, 0.015 rolling-resistance
coefficient and 1.15 baseline tire-friction coefficient. The engine then applies
road/environment modifiers. These values are not measured transmission or tire
data. MCT's efficiency-type multiplier is provisionally 1.0; existing transmission
types and their factors are unchanged. Continuous-torque automatic shifts,
converter multiplication and AWD torque-split dynamics are not newly modeled here.

Torque curves are piecewise-linear estimates constrained by rated torque and
horsepower (P = torque × angular speed); they are not digitized dyno curves.
Idle 800 rpm, shift targets, launch RPM/engagement/initial-transfer fractions are
estimates. V11 is the exact numeric record. Launch-control-enabled means the
model's launch strategy, not a reproduction of each manufacturer's controller.
Wheel radius is calculated from nominal driven tire dimensions and ignores
loaded deflection. Mass follows the cited curb/test reference without adding a
driver, fuel adjustment or options; matched testing must address this.

Next: obtain matched-condition stock tests and torque curves, quantify rollout
and test-mass differences, then validate launch and rolling acceleration separately.
Do not tune arbitrary power/grip multipliers simply to force a published time.
