# Backend Development Notes & Learnings

## 1. DTOs, MapStruct, and Lombok

* **MapStruct over ModelMapper:** We are using MapStruct instead of ModelMapper because our DTOs are implemented as Java
  `record`s. Since records do not have traditional getters and setters, ModelMapper fails to map them properly.
* **Compilation Order:** Both Lombok and MapStruct generate code during compile time, so the execution sequence is
  critical. **Lombok must run before MapStruct**.
* **Viewing Generated Code:** To inspect the classes generated at compile time, navigate to:
  `target/generated-sources/annotations/` -> *[your generated files]*

## 2. JPA & Database Design

* **`@Embeddable`:** This annotation defines a class whose properties are mapped directly into the database table of the
  owning entity, rather than having its own table. It is highly useful for creating composite keys (e.g., in a
  `ProjectMember` entity).
* **Composite Keys & `@MapsId`:** In the `ProjectMember` table, we use a composite key (`projectId` and `userId`).
  Because this is essentially a Many-to-Many relationship that requires additional columns (like `userRole`), we use the
  `@MapsId` concept to map the relationships directly to the composite key.
* **Schema Optimization:** If the `Project` table has an `owner` column, but the `ProjectMember` table already assigns
  the `OWNER` role to a user for that project, the `owner` column in the `Project` table is redundant and can be
  removed.

## 3. Validation: `@NotNull` vs. `@NotBlank`

| Annotation      | Null Check    | Empty String / Whitespace Check       | Applicable Types                                 | Note                                         |
|-----------------|---------------|---------------------------------------|--------------------------------------------------|----------------------------------------------|
| **`@NotNull`**  | Fails if null | **Allows** empty strings or spaces    | Any type (String, Integer, List, custom objects) |                                              |
| **`@NotBlank`** | Fails if null | **Fails** if empty or whitespace-only | Strings only                                     | Internally trims the string before checking. |

## 4. Exception Handling

* **`@RestControllerAdvice`:** This acts as a global exception-handling mechanism in Spring Boot. It allows you to
  intercept and handle exceptions across all `@RestController` classes in one centralized location, eliminating the need
  for repetitive `try-catch` blocks in every controller.

## 5. Database Indexing Checklist

Before adding an index to a column, ensure it meets these criteria:

* [x] Is this column frequently used in a `WHERE`, `JOIN`, or `ORDER BY` clause?
* [x] Does it have high cardinality (many unique values)?
* [x] Is the table large enough for indexes to actually improve performance?
* [x] Is the table relatively read-heavy (not extremely write-heavy)?
* [x] Are you frequently filtering by multiple columns together? *(If yes -> Use a **Composite Index**)*
* [x] Must the values be strictly unique? *(If yes -> Use a **Unique Index**)*

> **Tip on Composite Indexes:** The order of columns in the `columnList` matters immensely. Place the column with the
> highest cardinality (the one that filters out the most records) **first** in the index order.

## 6. Spring Security & JWT Architecture

* **Default Protection:** Simply adding the Spring Security dependency will protect all application routes by default.
* **Postman Testing:** When testing Basic Auth in Postman, you can easily add the username and password directly in the
  Authorization tab.

### The Stateless Security Context & Thread Pools

Because our application is completely **stateless** (using JWTs), the server retains no memory of a user between
requests. Therefore, the `SecurityContextHolder` is populated from scratch on *every single request*.

**The Request Lifecycle:**

1. **Request 1 Arrives:** The JWT filter intercepts the request, validates the token, extracts user details, and
   populates the `SecurityContextHolder`.
2. **Request 1 Finishes:** The moment the response is sent to the client, Spring Security completely destroys the
   `SecurityContextHolder` for that request.
3. **Request 2 Arrives:** The server has forgotten the user. The filter intercepts the new request, re-validates the
   token, and populates a brand new context.

**How the Tomcat Thread Pool is Related:**
When a Spring Boot app starts, Tomcat creates a pool of worker threads (default is 200) to handle incoming traffic.

* **The `ThreadLocal` Connection:** The `SecurityContextHolder` relies on a Java feature called `ThreadLocal`. This
  stores authentication data directly on the specific thread executing the request, allowing you to call
  `SecurityContextHolder.getContext().getAuthentication()` from anywhere in the service layer without passing the `User`
  object through method signatures.
* **Thread Borrowing:** When Request 1 arrives, Tomcat borrows an idle thread (e.g., `Thread-15`). Spring Security
  parses the JWT and places the `Authentication` object into `Thread-15`'s `ThreadLocal` storage.
* **The Danger of Thread Contamination:** Creating new threads is expensive, so when Request 1 finishes, `Thread-15` is
  returned to the pool for reuse. If Spring Security didn't wipe the context clean, the next user to borrow `Thread-15`
  might be accidentally authenticated as the previous user. This is a severe vulnerability known as **ThreadLocal
  Leakage**.

**Summary:** The thread pool dictates how security context is handled. Because threads are recycled, `ThreadLocal` data
must be strictly isolated. Spring Security guarantees this by populating the context at the start of a request and
aggressively wiping it clean the microsecond the thread is returned to the pool.

Here is a formatted snippet you can copy and paste directly into your `notes.md` file. I have kept it concise and
matched the style of your existing notes.

---

## 7. Postman: Environments & Automated Token Management

* **Environment Variables:** Instead of hardcoding URLs (like `http://localhost:8080`), create an Environment (e.g., "
  Local Dev") and set a `baseUrl` variable. You can then use `{{baseUrl}}/api/auth/login` across all requests.
* **Automating JWT Capture:** You don't need to manually copy and paste your token after logging in. In your Login
  request, go to the **Scripts -> Post-response** tab and add:

```javascript
const res = pm.response.json();
pm.environment.set("token", res.token);

```

*(Note: You must successfully send the Login request at least once for this script to run and save the token to your
environment).*

* **Passing the Token:** To use the captured token in protected routes, go to the **Authorization** tab, select **Bearer
  Token**, and type `{{token}}`. **Never** pass the token in the "Params" tab, as Spring Security expects it in the HTTP
  Headers, not the URL.

---

## 8 Stripe Sandbox

In a Stripe account, a Sandbox is an isolated testing environment that allows developers to build, test, and experiment
with Stripe's features without affecting your live production data or moving real money.

Historically, Stripe only offered a single "Test Mode" toggle for an entire account. However, as integrations grew more
complex, Stripe introduced the dedicated Sandboxes feature to give teams more control and flexibility over how they
test.

---

## 9 PostConstruct

Absolutely! Let’s step away from the technical jargon and use an analogy to make `@PostConstruct` completely clear.

Think of Spring as a moving company helping you move into a brand-new apartment.

### The "Moving In" Analogy

When Spring creates a new part of your application (a Bean), it happens in three distinct steps:

**1. The Constructor (Getting the Keys)**
This is when the apartment is officially yours. You open the door and walk in. The apartment *exists*, but it is
completely empty.

**2. Dependency Injection (The Movers Arrive)**
Next, Spring (the moving company) brings all your stuff inside. They bring in the fridge, the TV, and the sofa. In Java,
these are your "dependencies" (like your database connections or other services).

**3. `@PostConstruct` (Setting Up the House)**
Now that the furniture is inside, you can finally plug in the TV, put food in the fridge, and arrange your sofa pillows.

### The Problem It Solves

Imagine trying to put food in the fridge *before* the movers actually bring the fridge into the apartment (during Step
1). Your food would just fall on the floor!

In Java, if you try to use a database connection inside the **Constructor** (Step 1), your app will crash with a
`NullPointerException` because Spring hasn't brought the connection inside yet.

### What `@PostConstruct` Actually Is

`@PostConstruct` is simply a sticky note you put on a method to tell Spring:

> *"Hey, do not run this setup task until the movers have dropped off all my furniture!"*

It guarantees that all your tools, databases, and services are fully loaded, plugged in, and ready to be used before you
try to do anything with them.

### A Simple Translation

Here is how that looks in code:

```java

@Service
public class ApartmentService {

    private Fridge fridge; // A dependency

    // 1. Getting the keys
    public ApartmentService(Fridge fridge) {
        this.fridge = fridge; // The movers are bringing it in
    }

    // 2. Setting up
    @PostConstruct
    public void setup() {
        // Safe to use! We know 100% the fridge is here.
        fridge.addFood("Pizza");
    }
}

```

---

## 9 Stripe Payment flow

![img.png](img.png)

To understand webhooks, it helps to understand the problem they were invented to solve.

Let's step away from code for a second and use an everyday analogy: **Ordering a Pizza.**

### The Old Way: Standard APIs (Polling)

Imagine you order a pizza for delivery. You are very hungry, so you call the pizzeria 10 minutes later: *"Is my pizza
ready?"* They say: *"No."*
You call 5 minutes later: *"Is it ready now?"*
They say: *"No."*
You call 5 minutes later: *"How about now?"*

In software, this is called **Polling**. Your backend server constantly asks the Stripe server, *"Did the user pay yet?
Did the user pay yet?"* It works, but it is exhausting. It wastes your server's resources and clogs up Stripe's network
with useless questions.

### The Modern Way: Webhooks

Instead of constantly calling the pizzeria, you give them your phone number and say: **"Don't call me until the pizza is
ready. When it is, call this number."**

You go about your day. You can watch TV or clean the house. You don't have to think about the pizza. When it is finally
out of the oven, the pizzeria calls *you*.

In software, this is a **Webhook**.
A webhook is simply you (your backend) giving another service (Stripe) a specific URL (a "phone number") and saying: **"
Don't wait for me to ask. When a payment succeeds, send a message to this URL."**

---

### How It Works in Your Stripe App

Let's look at how this applies directly to the flowchart we just reviewed:

1. **The Setup:** In your Stripe Dashboard, you save a URL that points to your backend (e.g.,
   `https://api.your-lovable-clone.com/webhooks/stripe`).
2. **The Event:** A user is redirected to the Stripe Payment Gateway. They enter their credit card and hit "Pay".
3. **The Webhook Trigger:** Behind the scenes, Stripe contacts the credit card company, and the payment is approved.
4. **The Notification:** Stripe immediately creates an HTTP `POST` request containing a JSON body with the payment
   details and sends it directly to that URL you provided.
5. **The Database Update:** Your backend receives that POST request, sees that the payment was successful, and finally
   updates your database to grant the user access to "Plan A".

### Summary

A standard API is **"You asking them"** for data.
A webhook is a reverse API. It is **"Them telling you"** that an event just happened.

---

## 10 Stripe Checkout function explanation

This function is the exact implementation of **Step 2** from the flowchart we just looked at.

When your frontend says, *"Hey Backend, the user wants to subscribe to Plan A,"* this function's job is to contact
Stripe, build a custom checkout page for that specific user, and return the URL so the frontend can redirect them.

Let’s break it down chunk by chunk into plain English.

### 1. Finding the Plan

```java
Plan plan = planRepository.findById(request.planId()).orElseThrow(...);

```

First, your backend looks up "Plan A" in your own database using the ID sent from the frontend. It needs to find this
plan to get the **Stripe Price ID**.
*(Stripe doesn't know what your internal database ID `1` or `2` means. Stripe only understands its own unique IDs, which
usually look like `price_1Qx...`).*

### 2. Building the "Receipt"

```java
SessionCreateParams params = SessionCreateParams.builder()
        .addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L).build())

```

You are building the configuration for the Checkout Session.
The `LineItem` is exactly like an itemized receipt. You are telling Stripe: *"Put one item in the cart. The price for
this item matches this specific `StripePriceId`."* Stripe will look at that ID and automatically know how much to
charge (e.g., $10/month).

### 3. Setting the Rules

```java
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .

setSubscriptionData(...)

```

You are explicitly telling Stripe this is a recurring **Subscription**, not a one-time payment (like buying a t-shirt).
The `SubscriptionData` block configures how the recurring billing cycle behaves.

### 4. The Routing Instructions (The Bouncer)

```java
        .setSuccessUrl(frontendUrl +"/success.html?session_id={CHECKOUT_SESSION_ID}")
        .

setCancelUrl(frontendUrl +"/cancel.html")

```

Stripe's payment gateway is hosted on Stripe's servers, not yours. You have to tell Stripe where to send the user after
they are done.

* **Success:** If they pay successfully, send them to your success page. Stripe will automatically replace
  `{CHECKOUT_SESSION_ID}` with the real ID of the transaction so your frontend can show a confirmation message.
* **Cancel:** If they get to the credit card screen and click "Back" or close the window, send them to your cancel page
  so they can try again later.

### 5. The "Sticky Notes" (Crucial for Webhooks!)

```java
        .putMetadata("user_id",userId.toString())
        .

putMetadata("plan_id",plan.getId().

toString())
        .

build();

```

**This is the most important part of the function.** Remember how we talked about Webhooks calling your backend later?
When that Webhook arrives, Stripe is going to say, *"Hey, someone just paid $10!"* If you don't use metadata, your
backend will say, *"Great... but WHICH user was it, and WHAT plan did they buy?"*

By putting `Metadata` here, you are attaching invisible sticky notes to the transaction. When the Webhook eventually
fires, Stripe will hand those exact sticky notes back to you so you can successfully update your database.

### 6. Making the Call

```java
    try{
Session session = Session.create(params);
        return new

CheckoutResponse(session.getUrl());
        }catch(
StripeException e){
        throw new

RuntimeException(e);
    }
            }

```

Up until this point, everything was just configuration.
`Session.create(params)` actually makes the HTTP network call to the Stripe API. Stripe generates a secure, unique
webpage just for this transaction and returns the URL.
Finally, you wrap that URL in a `CheckoutResponse` and send it back to your Angular frontend!

---

## 10 What is difference between security filter and spring mvc

### How Spring Boot Handles HTTP Requests: Filters vs. Spring MVC

To understand why global exception handlers sometimes fail to catch security errors, you have to look at how a Java web
server (like Tomcat, which runs inside Spring Boot) handles incoming requests. **Filters and Spring MVC are two
completely different layers of the application.**

#### The Nightclub Analogy

Imagine your Spring Boot application is an exclusive nightclub.

**1. The Security Filter (Outside the Club)**
Filters are the **bouncers standing outside the front door**.

* They don't care what kind of music is playing inside, what you want to drink, or who your favorite bartender is.
* Their *only* job is to look at your ID (your JWT token).
* If your ID is fake or expired, they kick you out immediately (`401 Unauthorized`).
* Because you are outside on the sidewalk, the manager inside the club never even knows you were there.

**2. Spring MVC (Inside the Club)**
Spring MVC is the entire operation *inside* the building.

* The **`DispatcherServlet`** is the Host at the front desk. Once the bouncers let you in, the Host asks, "Where do you
  want to go?" and routes you to the correct table.
* The **Controllers (`@RestController`)** are the Waiters. They take your specific order (e.g., "Create a project," "
  Invite a member").
* The **`@RestControllerAdvice`** is the Floor Manager. If a waiter drops your food or messes up your order, the Manager
  comes over to handle the apology and fix it.

---

### Why JWT Exceptions Aren't Caught by Default

When a user sends an expired token, your `JwtAuthFilter` (the bouncer) catches it and throws an error while the user is
still "outside on the sidewalk."

Your `@RestControllerAdvice` (the Manager) only handles problems that happen *inside* the club (inside Spring MVC).
Because the request never made it past the front door, the Manager never heard the exception.

> **The Solution: `HandlerExceptionResolver**`
> This is why we have to inject the `HandlerExceptionResolver` into the filter. It acts as a **walkie-talkie**, allowing
> the bouncer outside to radio the manager inside and say, *"Hey, I'm kicking this guy out, come handle the paperwork."*

---

## 11 Stripe CLI

**Yes, exactly! You nailed it.** To understand exactly *why* we need the CLI, you have to look at the networking problem
of local development.

### The Problem: The Invisible Server

Right now, your Spring Boot server is running on `localhost:8080`.
`localhost` literally means "this specific computer." Your computer is sitting safely behind your home Wi-Fi router,
which has a firewall designed to block random traffic from the internet.

If a customer pays in your Stripe Sandbox, Stripe says: *"Great! Let's send the Webhook! ...Wait, where is the server?"*
Stripe is up in the cloud. It cannot see your laptop, and your router will block Stripe if it tries to knock on your
door.

### The Solution: The CLI Tunnel

When you run `stripe listen --forward-to host.docker.internal:8080...` inside your Docker container, the Stripe CLI acts
like a dedicated telephone line.

1. The CLI reaches **out** through your router to Stripe and says, *"Keep this connection open."* (Routers allow
   outgoing connections).
2. When a payment happens, Stripe doesn't try to find your computer. It just sends the webhook down that open telephone
   line directly to the CLI.
3. The CLI receives it, turns around, and hands it directly to your Spring Boot server running on port `8080`.

Without the CLI, you would have to deploy your backend to a public server (like AWS or Heroku) every single time you
wanted to test a payment!

---

## 12 Stripe vs Ngrok

Since we just talked about how the Stripe CLI forwards webhooks to your Spring Boot app, you actually already understand
the exact concept of ngrok without realizing it!

Here is the straightforward explanation of what ngrok is and why developers use it.

### What is ngrok?

**Ngrok** (pronounced *en-grok*) is a tool that creates a secure, temporary tunnel from the public internet directly to
your local computer (your `localhost`).

It allows anyone in the world to access a website or API running on your laptop, even though your laptop is hidden
safely behind your home Wi-Fi router and firewall.

### The P.O. Box Analogy

Imagine your Spring Boot server is a secret underground bunker. Nobody on the outside world has the address, and the
front door is locked (your router's firewall).

If you run ngrok, it acts like renting a **public P.O. Box**.

1. Ngrok gives you a public, temporary web address (e.g., `https://random-word-123.ngrok.app`).
2. You give that public address to a friend, or to a service like GitHub or Twilio.
3. When they send a request to that public address, ngrok instantly acts as a delivery driver. It takes the package,
   travels down a secure tunnel straight through your firewall, and drops it off at your secret bunker (
   `localhost:8080`).

### Ngrok vs. Stripe CLI

If you are wondering how this relates to what we just did with Stripe: **The Stripe CLI is basically a mini,
custom-built version of ngrok just for Stripe.**

* **Stripe CLI:** Creates a tunnel *only* for Stripe webhooks. It listens for Stripe events and forwards them to your
  local app.
* **Ngrok:** Creates a universal tunnel for *anything*.

### Why do developers use it?

1. **Testing Webhooks:** If you are integrating with PayPal, Twilio (SMS), or GitHub, they all need a public URL to send
   webhooks to. Since they don't have their own custom CLI like Stripe does, you use ngrok to give them a temporary
   public URL to hit your local code.
2. **Showing Work to Clients:** If you built a website on your laptop and want to show your boss or client, you don't
   have to deploy it to AWS or a real server. You just run ngrok, send them the `ngrok.app` link, and they can browse
   the site directly from your laptop.
3. **Mobile App Testing:** If you are building an Android or iOS app that needs to talk to your Spring Boot backend, the
   phone can't always easily reach `localhost`. Ngrok gives the mobile app a real internet URL to connect to.
