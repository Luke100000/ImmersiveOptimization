# Immersive Optimization

An entity tick scheduler to massively reduce lag and resource requirements of entities.

Unlike similar projects, this mod will not disable ticks all together, allowing farms or distant entities to still
function.

## Features

* Gradually slows down tick rate of entities and block entities
    * When occluded by blocks (currently disabled since Forge hates me, and because effect is marginal)
    * When out of viewport
    * When far away
* Distributes updates over multiple ticks to flatten lag spikes
* Dynamically adjusts tick rate based on server load

Thanks [OcclusionCulling](https://github.com/LogisticsCraft/OcclusionCulling) for the nice occlusion culling library!