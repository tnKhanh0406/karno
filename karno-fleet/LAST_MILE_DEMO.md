# Last-mile clustering demo

The fixture `src/main/resources/fixtures/last-mile-orders.json` contains 200 synthetic
orders in one assumed shipper zone in Hanoi: 24 EXPRESS and 176 STANDARD.
Coordinates are demo points, not verified delivery addresses. Weight is kg and volume
is m3. The fixture includes dense areas, isolated points and a chain of nearby points.

## API

- `GET /api/last-mile/clusters/demo?maxDiameterMeters=200`: cluster the bundled fixture.
- `POST /api/last-mile/clusters?maxDiameterMeters=200`: cluster a JSON array in the same
  format as the fixture (maximum 1000 orders).
- `POST /api/last-mile/load?shipperUserId={uuid}&maxDiameterMeters=200`: cluster the
  submitted orders and select the load for the shipper's active vehicle. The vehicle
  capacity is read from the same first-mile context used by pickup planning.
- `POST /api/last-mile/load/demo?shipperUserId={uuid}&maxDiameterMeters=200`: use
  `fixtures/last-mile-orders.json` directly; no request body is required.
- `POST /api/last-mile/plan?shipperUserId={uuid}&maxDiameterMeters=200&plannedStartAt=2026-10-14T08:00:00`:
  run last-mile CFI planning with HERE Matrix for a submitted order list.
- `POST /api/last-mile/plan/demo?shipperUserId={uuid}&maxDiameterMeters=200&plannedStartAt=2026-10-14T08:00:00`:
  run the same planning flow using the bundled fixture; no request body is required.

Response shape:

```json
{
  "maxDiameterMeters": 200,
  "totalOrders": 200,
  "express": [{"clusterId": "EXPRESS-1", "location": {"latitude": 21.0285, "longitude": 105.8542}, "orders": []}],
  "standard": [{"clusterId": "STANDARD-1", "location": {"latitude": 21.0285, "longitude": 105.8542}, "groups": [
    {"deliveryWindow": "ADMIN", "orders": []},
    {"deliveryWindow": "OUTSIDE", "orders": []}
  ]}]
}
```

Arrays above illustrate the shape; actual responses contain full order objects and
omit empty window groups. EXPRESS requires a null deliveryWindow. STANDARD requires
ADMIN (08:00 inclusive to 17:00 exclusive) or OUTSIDE (before 08:00 or from 17:00).
This step groups preferences only; it does not check delivery feasibility at the current time.

Priority pools are separated before geographic clustering. Complete-linkage merges
the closest eligible clusters while keeping every pair of locations within the
configured diameter, using Haversine distances. Distances are computed once;
cluster distances use max updates and a priority queue: O(n² log n) time and O(n²)
space. No routing API is called. IDs and ties are deterministic for identical inputs,
but cluster IDs are not persistent identities across changed datasets.

Each cluster has a shared `location` for future routing: the actual order location
minimizing the sum of Haversine distances to all orders (medoid, weighted by order count).
Ties use order ID; singleton clusters use their only order location. STANDARD uses
all its orders across both windows to choose one shared location. This is an approximation
for routing between clusters, not a verified road access point or a replacement for
individual delivery coordinates.

Window grouping happens after geographic clustering. There is no building-based override,
route ordering or persistence. The load response is an ordered delivery plan, not a capacity fill operation.
Express clusters are ranked first by the earliest order creation time in each cluster.
Standard clusters follow after Express; each Standard cluster is split into separate
`ADMIN` and `OUTSIDE` clusters, each ranked by its earliest order creation time.
All orders are retained, and the response contains the ordered cluster/order lists,
the representative `location` (`latitude` and `longitude`) of each cluster, and their
total weight and volume. The `ADMIN` and `OUTSIDE` sub-clusters retain the geographic
representative location of their original Standard cluster. Vehicle capacity fields
remain in the response for context, but do not filter the plan. The service-time rule of 10 minutes per cluster
visit plus 1 minute per delivered order belongs to the later route phase. First-mile is
unchanged. Running the full application still requires its existing DB and environment
configuration; the clustering and selector unit tests do not require them.

The separate `/api/last-mile/plan` endpoints use the ordered clusters from the load
service as CFI input. Every cluster is tried at every route insertion position. The
candidate route must fit vehicle weight/volume and return before the shift ends. HERE
Matrix provides the travel-time provider for all candidate evaluations. `ADMIN` uses
08:00 inclusive to 17:00 exclusive, while `OUTSIDE` uses 17:00 to 22:00. Clusters
that cannot be inserted are returned in `unassignedClusters` with a reason.
Last-mile planning enforces Express precedence: a Standard cluster can only be
inserted after every Express cluster already accepted in the route. If an Express
cluster cannot be assigned, subsequent Standard clusters are reported as
`EXPRESS_PRECEDENCE_BLOCKED`. The planning response also includes HERE route geometry
and per-cluster `arrivalAt`, `serviceStartAt`, and `departureAt` timestamps.

Run with JDK 21: `mvn -pl karno-fleet -Dtest=LastMileClusteringTest test`.
