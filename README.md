# dynamo-utils

WIP:: The code in this repo at the moment is basically my experiments
in the REPL. Im still shaping a lot of the details including the API.

Contains core set of functions to perform the following CRUD operations.

- create-with-hash-key
- create-with-hash-and-range-key
- delete-by-hash-key
- delete-by-hash-and-range-key
- update-by-hash-key
- update-by-hash-and-range-key
- get-by-hash-key
- get-by-hash-and-range-key

A macro `defmodel` that takes a table-spec and generates custom
functions for the entity. For eg:
Table:  Users (hash-key=user-id)
Index1: User-Email (hash-key=email-id)
Index2: User-Dept  (hash-key=department-id, range-key=user-id)

the following functions must be generated: (tbl-spec and creds are arg0 and arg1)
- (create user-id user)
- (update user-id user-updates)
- (find-by-user-id user-id)
- (delete user-id)
- (find-by-email-id email-id)
- (find-by-department-id-and-user-id department-id email-id)
- (find-by-department-id department-id)


Notes:
- get functions must take an optional `index` name and use that index when supplied.
- get functions will support a `filter` argument.
- get functions will support a `expression` argument.
- all functions will take `table-spec` as first arg and `creds` map as the second argument.
- all update functions will use mvcc by default.
- core functions will not be 'deep' - for eg., update functions will
  not support conditional updates, but will support in-place updates
  (over-write only, numeric operators etc will not be
  supported). Users can implement special cases by overriding
  (redeffing) these functions.
- all get functions will support projections.


## License

Copyright © 2015-2018 Sathyavijayan Vittal

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
