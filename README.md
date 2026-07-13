# Immersive Optimization

An entity tick scheduler designed to reduce server load by lowering the tick rate of distant entities.

Unlike optimizers that completely stop entity ticking, Immersive Optimization continues ticking affected entities at
a reduced rate so distant entities and farms can continue to function.

## Features

* Gradually reduces entity tick rates based on distance
* Further reduces tick rates for:
    * Entities outside the tracking range
    * Entities outside the camera viewport in single-player
* Distributes entity updates across ticks to reduce lag spikes
* Dynamically adjusts entity tick rates based on server load
* Optional distance-based block-entity optimization, disabled by default for compatibility
