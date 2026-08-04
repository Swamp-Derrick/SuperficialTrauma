# Superficial Trauma design notes

## Downed presentation decision

The downed state is server-authoritative, but its camera and model animation are client presentation only. Visual animation must not rotate or move the authoritative server player position.

### Third-person presentation

- Do not use the vanilla sleeping state or force `Pose.SLEEPING`. Bed orientation, wake-up behavior, entity dimensions, eye height, and other mods' sleep checks make it an unsafe foundation.
- The first implementation should apply a dedicated render-only transform to the whole player model. It will rotate and lower the skin, armor, outer skin layers, cape, and held item together into a stable horizontal pose.
- Store a fixed downed body yaw so the model does not rotate when the victim moves their mouse, reconnects, changes dimension, or enters another player's tracking range.
- A render-only pose does not change the hitbox. Downed targeting and interaction dimensions must be designed separately instead of silently inheriting the small vanilla sleeping hitbox.
- A fully animated custom limb pose is optional later polish. Begin with a rigid whole-model pose; later model-part adjustments may add bent limbs, slumping, and breathing after CGM weapon and armor compatibility is understood.

### First-person transition

- Standing and crouching use different starting camera heights but share four fall profiles: forward, backward, left, and right.
- Sprinting uses horizontal movement velocity as the fall direction. Walking or stationary players fall away from the incoming hit, using projectile velocity first and source position as fallback.
- Swimming, crawling, vehicles, climbing, elytra flight, sleeping, cramped spaces, or an unknown safe camera path use a direct fade instead.
- The transition should last about 0.9 seconds: short impact hesitation, eased camera fall, then a black fade into the downed interface.
- Provide full, reduced-roll, and fade-only client options for motion sensitivity.

### Planned implementation order

1. Persist downing time, body yaw, movement posture, and four-way fall direction.
2. Synchronize a compact downed-pose snapshot to the victim and tracking players, including late trackers.
3. Add the rigid third-person render pose and fade-only victim transition.
4. Add four first-person camera profiles and safe-state fallbacks.
5. Polish limb positioning, CGM weapon placement, camera collision, and accessibility settings.

## Downed damage rule

While incapacitated, awakening, in cardiac arrest, or in ventricular fibrillation, positive final damage does not create or update a trauma instance and does not add traumatic pain or stress.

- Incapacitated or awakening: shorten the remaining time before cardiac arrest by `10 × D` seconds.
- Cardiac arrest or ventricular fibrillation: shorten the remaining time before brain death by `10 × D` seconds.
- Awakening damage first cancels awakening, then applies the incapacitated rule.
- One hit only shortens the countdown belonging to the state in which that hit began; excess shortening does not spill into the next state.
- The server stores absolute deadlines. The client only displays the remaining time and never controls a state transition.
