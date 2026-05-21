1. We are using map struct instead of model mapper because for dto we are using records instead of class.
2. Remember lombok should run b4 map struct.
3. Both generate while compile time, so sequence matters.
4. To see all generated classes while compiling target->generated-sources->annotations->generated file
5. records don't have getters and setters that why model mapper does not work
6. @Embeddable annotation is used to define a class whose properties are meant to be mapped directly into the database table of the entity that owns it, rather than having its own separate table.Useful for making composite keys in table look into project member entity.
7. Now in project member table we have composite key project id and user id. So in this we are using concept of mapsId.
8. project member should be many to many with only 2 columns but i also want other columns like userRole in that project so thats why mapsId concept is used.
9. @NotNull
Ensures the field is not null.
Does not check for empty strings or whitespace.
Works on any type (String, Integer, List, custom objects, etc.).
10. @NotBlank
Ensures the field is not null, not empty, and not whitespace-only.
Works on Strings only.
Internally trims the string before checking.
11. @RestControllerAdvice is a global exception handling mechanism in Spring Boot. It allows you to handle exceptions across all @RestController classes in one centralized place, instead of writing try-catch blocks in every controller.
12. While adding indexes
    ✅ Is this column in a WHERE / JOIN / ORDER BY clause?
    ✅ Does it have high cardinality (many unique values)?
    ✅ Is this table large enough for indexes to matter?
    ✅ Is this table not extremely write-heavy?
    ✅ Are you combining columns that are always queried together? → Composite
    ✅ Must values be unique? → Unique index
13. Order in index matters inside the columnList
So in this order keep the first column such that filters out most of the things.
14. In project table we have column of owner but we also have table of projectMember which has role of OWNER. So we can remove from that.
15. after adding spring security by default all routes are protected
16. in postman in basic auth you can add user and password in token
17. 
