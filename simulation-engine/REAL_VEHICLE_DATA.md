# Initial real-vehicle validation data

These fixtures exist to measure the baseline model's error. They are not yet
production catalog records. Values are classified so an estimate cannot be
mistaken for a verified specification.

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
