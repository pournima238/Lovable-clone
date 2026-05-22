
# Backend Development Notes & Learnings

## 1. DTOs, MapStruct, and Lombok

* **MapStruct over ModelMapper:** We are using MapStruct instead of ModelMapper because our DTOs are implemented as Java `record`s. Since records do not have traditional getters and setters, ModelMapper fails to map them properly.
* **Compilation Order:** Both Lombok and MapStruct generate code during compile time, so the execution sequence is critical. **Lombok must run before MapStruct**.
* **Viewing Generated Code:** To inspect the classes generated at compile time, navigate to:
  `target/generated-sources/annotations/` -> *[your generated files]*

## 2. JPA & Database Design

* **`@Embeddable`:** This annotation defines a class whose properties are mapped directly into the database table of the owning entity, rather than having its own table. It is highly useful for creating composite keys (e.g., in a `ProjectMember` entity).
* **Composite Keys & `@MapsId`:** In the `ProjectMember` table, we use a composite key (`projectId` and `userId`). Because this is essentially a Many-to-Many relationship that requires additional columns (like `userRole`), we use the `@MapsId` concept to map the relationships directly to the composite key.
* **Schema Optimization:** If the `Project` table has an `owner` column, but the `ProjectMember` table already assigns the `OWNER` role to a user for that project, the `owner` column in the `Project` table is redundant and can be removed.

## 3. Validation: `@NotNull` vs. `@NotBlank`

| Annotation | Null Check | Empty String / Whitespace Check | Applicable Types | Note |
| --- | --- | --- | --- | --- |
| **`@NotNull`** | Fails if null | **Allows** empty strings or spaces | Any type (String, Integer, List, custom objects) |  |
| **`@NotBlank`** | Fails if null | **Fails** if empty or whitespace-only | Strings only | Internally trims the string before checking. |

## 4. Exception Handling

* **`@RestControllerAdvice`:** This acts as a global exception-handling mechanism in Spring Boot. It allows you to intercept and handle exceptions across all `@RestController` classes in one centralized location, eliminating the need for repetitive `try-catch` blocks in every controller.

## 5. Database Indexing Checklist

Before adding an index to a column, ensure it meets these criteria:

* [x] Is this column frequently used in a `WHERE`, `JOIN`, or `ORDER BY` clause?
* [x] Does it have high cardinality (many unique values)?
* [x] Is the table large enough for indexes to actually improve performance?
* [x] Is the table relatively read-heavy (not extremely write-heavy)?
* [x] Are you frequently filtering by multiple columns together? *(If yes -> Use a **Composite Index**)*
* [x] Must the values be strictly unique? *(If yes -> Use a **Unique Index**)*

> **Tip on Composite Indexes:** The order of columns in the `columnList` matters immensely. Place the column with the highest cardinality (the one that filters out the most records) **first** in the index order.

## 6. Spring Security & JWT Architecture

* **Default Protection:** Simply adding the Spring Security dependency will protect all application routes by default.
* **Postman Testing:** When testing Basic Auth in Postman, you can easily add the username and password directly in the Authorization tab.

### The Stateless Security Context & Thread Pools

Because our application is completely **stateless** (using JWTs), the server retains no memory of a user between requests. Therefore, the `SecurityContextHolder` is populated from scratch on *every single request*.

**The Request Lifecycle:**

1. **Request 1 Arrives:** The JWT filter intercepts the request, validates the token, extracts user details, and populates the `SecurityContextHolder`.
2. **Request 1 Finishes:** The moment the response is sent to the client, Spring Security completely destroys the `SecurityContextHolder` for that request.
3. **Request 2 Arrives:** The server has forgotten the user. The filter intercepts the new request, re-validates the token, and populates a brand new context.

**How the Tomcat Thread Pool is Related:**
When a Spring Boot app starts, Tomcat creates a pool of worker threads (default is 200) to handle incoming traffic.

* **The `ThreadLocal` Connection:** The `SecurityContextHolder` relies on a Java feature called `ThreadLocal`. This stores authentication data directly on the specific thread executing the request, allowing you to call `SecurityContextHolder.getContext().getAuthentication()` from anywhere in the service layer without passing the `User` object through method signatures.
* **Thread Borrowing:** When Request 1 arrives, Tomcat borrows an idle thread (e.g., `Thread-15`). Spring Security parses the JWT and places the `Authentication` object into `Thread-15`'s `ThreadLocal` storage.
* **The Danger of Thread Contamination:** Creating new threads is expensive, so when Request 1 finishes, `Thread-15` is returned to the pool for reuse. If Spring Security didn't wipe the context clean, the next user to borrow `Thread-15` might be accidentally authenticated as the previous user. This is a severe vulnerability known as **ThreadLocal Leakage**.

**Summary:** The thread pool dictates how security context is handled. Because threads are recycled, `ThreadLocal` data must be strictly isolated. Spring Security guarantees this by populating the context at the start of a request and aggressively wiping it clean the microsecond the thread is returned to the pool.

Here is a formatted snippet you can copy and paste directly into your `notes.md` file. I have kept it concise and matched the style of your existing notes.

---

## 7. Postman: Environments & Automated Token Management

* **Environment Variables:** Instead of hardcoding URLs (like `http://localhost:8080`), create an Environment (e.g., "Local Dev") and set a `baseUrl` variable. You can then use `{{baseUrl}}/api/auth/login` across all requests.
* **Automating JWT Capture:** You don't need to manually copy and paste your token after logging in. In your Login request, go to the **Scripts -> Post-response** tab and add:
```javascript
const res = pm.response.json();
pm.environment.set("token", res.token);

```


*(Note: You must successfully send the Login request at least once for this script to run and save the token to your environment).*
* **Passing the Token:** To use the captured token in protected routes, go to the **Authorization** tab, select **Bearer Token**, and type `{{token}}`. **Never** pass the token in the "Params" tab, as Spring Security expects it in the HTTP Headers, not the URL.

---