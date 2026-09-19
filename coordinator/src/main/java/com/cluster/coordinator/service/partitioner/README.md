# Job Partitioning

## Purpose
The `partitioner` package is responsible for breaking down large, monolithic distributed compute Jobs into smaller, discrete `Task` entities that can be independently scheduled and executed across multiple Worker Agents.

## Responsibilities
- Defining the strategy interface for job subdivision.
- Evaluating the `requestedCpu` or parameters of a Job to determine the optimal number of partitions.
- Producing the list of `Task` records required to complete a Job.

## Architecture / Flow
When `JobService` receives a new Job request, it queries its list of injected `JobPartitioner` implementations. It selects the first partitioner that `supports()` the given `taskType`. The partitioner then generates `Task` entities which are persisted and eventually picked up by the `SchedulerService`.

## Key Files

| File | Responsibility |
|------|----------------|
| `JobPartitioner.java` | The core interface defining the `supports(taskType)` and `partition()` contract. |
| `SumRangeJobPartitioner.java` | A concrete implementation that partitions a large mathematical range-sum job into distinct sub-ranges. |

## Integration
Partitioners are strictly utilized by the `JobService` during the initial `PARTITIONING` phase of the Job lifecycle.
