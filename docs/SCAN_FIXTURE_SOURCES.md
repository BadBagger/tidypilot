# Room Scan Online Fixture Sources

These sources are used as an online reference set for TidyPilot's local room
photo scan tests. The app does not fetch these images at runtime, and fixture
downloads should stay in `.local-scan-fixtures/`, which is ignored by git.

The tests commit only room type, observed visible-context notes, expected mess
level, and expected issue tags. Do not commit downloaded photos unless the
license and privacy review are explicit.

## Reference Fixtures

| Fixture | Source | Room | Expected signal |
| --- | --- | --- | --- |
| `messy_kitchen_sink_commons` | https://commons.wikimedia.org/wiki/File:Messy_kitchen_sink.jpg | Kitchen | dishes, sink, counter reset |
| `gfp_messy_kitchen_sink_commons` | https://commons.wikimedia.org/wiki/File:Gfp-messy-kitchen-sink.jpg | Kitchen | dishes, sink, heavier kitchen reset |
| `messy_bedroom_bag_commons` | https://commons.wikimedia.org/wiki/Category:Bedrooms_in_the_United_States | Bedroom | bed reset, floor/laundry/trash context |
| `cluttered_bedroom_commons` | https://commons.wikimedia.org/wiki/Category:Bedrooms_in_the_United_States | Bedroom | unmade bed, floor clutter, bedroom surfaces |
| `laundry_room_pile_commons` | https://commons.wikimedia.org/wiki/File:Energy_use_in_the_laundry_room_(36377510063).jpg | Laundry | laundry load, fold/sort pile |
| `cluttered_home_office_kitchen_commons` | https://commons.wikimedia.org/wiki/File:DFC_3009_A_cozy_lived-in_home_office_and_kitchen_area_with_a_cluttered_desk_display_shelves_of_dishes_and_ornaments_a_refrigerator_and_stools_scattered_across_a_concrete_floor.jpg | Office | desk/surface clutter, floor clutter |
| `cluttered_kitchen_counter_commons` | https://commons.wikimedia.org/wiki/Category:Microwave_ovens | Kitchen | counter clutter, boxes/tools, wipe/reset |
| `cluttered_utility_room_commons` | https://commons.wikimedia.org/wiki/Category:Fire_extinguishers_in_the_United_States | Storage | equipment, cords, floor path clutter |

## Local Download Notes

For manual QA, download copies into:

```text
.local-scan-fixtures/photos/
```

Keep that folder local. The tests are intentionally metadata-driven until a
real on-device vision model reads pixels directly.
