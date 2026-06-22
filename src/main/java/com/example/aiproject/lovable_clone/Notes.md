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

---

## 13 Stripe Flow

Based on the flowchart in the image, here is a step-by-step explanation of how the subscription payment process works
using Stripe:

### Overview

The diagram illustrates a standard checkout flow where a user subscribes to a plan using Stripe Checkout. It involves
your Frontend (**FE**), your Backend (**BE**), and Stripe's infrastructure.

### The Step-by-Step Flow

**1. The User Initiates the Subscription**

* **Action:** The user clicks a button on the Frontend (**FE**) to `subscribe to plan : planA`.
* *Context:* This is the starting point, as mentioned in the video captions ("front-end will basically click a button
  somewhere...").

**2. Frontend Requests Checkout Session**

* **Action:** The **FE** sends an API request to your Backend (**BE**) telling it that the user wants to buy "planA".

**3. Backend Communicates with Stripe**

* **Action:** The **BE** uses the **Stripe SDK** to communicate with the main **Stripe** servers. It asks Stripe to
  create a checkout session (`request a sessionUrl`).

**4. Stripe Returns the Session URL**

* **Action:** Stripe successfully creates a secure, hosted checkout page for this specific transaction and returns the
  unique `sessionUrl` back to your **BE**.

**5. Backend Forwards the URL**

* **Action:** Your **BE** takes that `sessionUrl` and sends it back to your **FE** (`url`).

**6. Redirect to Payment Gateway**

* **Action:** Your **FE** redirects the user's browser to the **Stripe Payment Gateway**.
* *Context:* This is the secure Stripe page where the user actually types in their credit card details. This keeps
  sensitive payment data off your own servers.

**7. Return to the Frontend**

* **Action:** Once the user completes the payment, the Stripe Payment Gateway redirects the user back to your **FE**
  using a specific `successUrl` (e.g., a "Thank You" or "Payment Successful" page on your website).

**8. Asynchronous Webhooks**

* **Action:** Around the same time the user is redirected, the main **Stripe** server securely communicates directly
  with your **BE** via **webhooks**.
* *Context:* This is how Stripe officially informs your backend that the payment was successful so your backend can
  safely update the user's database record to show they are now subscribed to "planA".

To make stripe cli work which basically listens events and tells to our localhost from our docker the command is this ->
docker run --rm -it -v "C:\Users\Pournima Thakare\.config\stripe:/root/.config/stripe" stripe/stripe-cli:latest listen
--forward-to host.docker.internal:8080/webhooks/payment

---

## 14 Subscription and Plan

* One user can have only one subscription. if a user alresy has a subscrition it will take it to customer portal.
* ![img_1.png](img_1.png)
* Our subscription table has stripeSubscriptionId
* Also our Plan has stripe PlanId
* Note susbscription.deleted event will be triggered only at the end of billing cycle
* The Stripe Customer Portal is a secure, pre-built web page hosted by Stripe that allows your customers to manage their
  own subscriptions and billing details.
* Instead of you having to write backend logic and build complex front-end interfaces for billing management from
  scratch, Stripe provides a ready-made, customizable portal that you can plug directly into your application.
* In dashboard settings we have billing inside which we have settings for customer portal
* In the Stripe Java SDK, a "session params" object (formally named SessionCreateParams) is a configuration object used
  to securely bundle all the settings and data you want to send to Stripe to generate a new session URL. Instead of
  forcing you to pass a dozen different arguments into a single method call, or manually write a massive JSON payload,
  the Stripe SDK uses the Builder design pattern to create these parameter objects.
* So remember when you cancel subscription the current subscription still remains active until the period end.
* After period ends then the subscription is deleted that is its status becomes cancelled.

---

## 15 Spring AI

**Spring AI** is an official project within the Spring ecosystem designed to make it easy for Java developers to build
AI-powered applications.

You can think of it as doing for AI what **Spring Data** did for databases. Just like Spring Data lets you swap out
MySQL for PostgreSQL without rewriting all your database code, Spring AI lets you swap out OpenAI for Google Gemini,
Anthropic, or a local Ollama model without rewriting all your AI logic.

### Key Features & Concepts

* **Portable API (The "Write Once" philosophy):** Instead of using the specific, proprietary SDKs for OpenAI, Google, or
  Anthropic, Spring AI gives you a unified `ChatClient` interface. You send a standard `Prompt` object, and Spring AI
  translates it into the specific API format of the AI provider you configure in your `application.properties`.
* **Retrieval-Augmented Generation (RAG):** If you want an AI to answer questions based on your own private PDFs,
  databases, or company documents (instead of just its general internet knowledge), Spring AI provides out-of-the-box
  tools for:
    * **Document Readers:** To ingest PDFs, JSON, text files, etc.
    * **Document Splitters:** To chunk large documents into smaller, searchable pieces.
    * **Vector Store Integrations:** Native support for storing and searching those chunks in vector databases like
      PgVector, Pinecone, Chroma, and Neo4j.
* **Function Calling (Tools):** This allows you to write standard Java methods (e.g., `getWeather(String city)` or
  `cancelSubscription(String userId)`), annotate them with `@Bean` and `@Description`, and pass them to the AI. The AI
  can then "decide" to call your Java code during a conversation to fetch real-time data or perform actions.
* **Structured Output:** Instead of parsing raw text strings returned by an LLM, Spring AI can force the model to return
  data that perfectly maps to a Java `Record` or `Class` (e.g., asking the AI to extract data from a receipt and return
  a `ReceiptDTO` object directly).

### A Simple Implementation Example

If you wanted to add Spring AI to your `lovable-clone` project, a basic chat endpoint looks as simple as this:

@RestController
public class ChatController {

    private final ChatClient chatClient;

    // Spring AI automatically provides the ChatClient.Builder based on your properties
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ask")
    public String askAi(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content(); // Returns the string response from the AI
    }

}---

## 16 AI-Powered Coding Assistant Architecture

![img_2.png](img_2.png)
This diagram represents the complete backend flow for an AI-powered coding assistant, showing how a user's prompt goes
from the frontend, gets enriched with context, is processed by an LLM, and streams back while being saved.

### 1. The Request (Frontend to Spring Boot)

* **The Action:** The user types a prompt in the React Frontend (e.g., "update the color of ProfileCard button to Red").
* **The API Call:** A POST request is sent to your **Spring Boot Server** containing this `userPrompt`.

### 2. Context Assembly (Preparing the Prompt)

Before the server sends the prompt to the LLM, it must give the LLM context about the project.

* **System Prompt:** It attaches a set of hidden instructions (`guardrails/constraints/rules`). For example, telling the
  AI how to format its response using XML tags instead of JSON.
* **Get the File Tree:** It fetches the current structure of the user's project (e.g., a list of files). The LLM needs
  to know what files exist so it knows what to edit.

### 3. The LLM & Tool Calling (The Brain)

The combined data is sent to the LLM.

* **The Context Window:** The total input (`Input_context_tokens`) equals the `userPrompt` + `System Prompt` +
  `file_tree` + any actual `files_content`.
* **The Tool (`get_file_content`):** If the LLM looks at the file tree and realizes it needs to see specific code (like
  `ProfileCard.tsx`), it triggers a Tool/Function call.
* **Fetching Data:** The tool requests the file contents from the **Template Bucket (Minio)**. A **circuit-breaker** is
  used here as a safety mechanism to stop the LLM from entering an infinite loop of requesting files or crashing if
  Minio goes down.

### 4. Streaming the Response

* Once the LLM figures out the code changes, it starts generating the response.
* Instead of waiting for the whole response to finish, it uses a **streaming response**.
* The LLM streams chunks to the Spring Boot server, and Spring Boot immediately forwards that stream via Server-Sent
  Events (SSE) or WebSockets back to the React Frontend so the user sees the code typing out in real-time.

### 5. Parsing & Storage (Background Work)

While the stream is being sent to the user, the Spring Boot Server is doing heavy lifting in the background:

* **Buffering:** It uses a `StringBuilder` to collect all the streamed chunks into one complete string. It also
  calculates token usage for billing.
* **Parsing the Output:** It looks for specific XML-style tags in the AI's response:
    * `<message>...</message>`: Text meant to be read by the user.
    * `<file name="src/App.tsx">...</file>`: The actual code changes.
* **Saving to Database (PostgreSQL):** The text `<message>` (Assistant Message) and the **metadata** of the files are
  saved instantly to your relational database.
* **Saving to Object Storage (Minio/S3):** The actual, heavy code contents (`<file>`) are saved into Minio buckets.
  Writing to Minio is a slower operation compared to the DB, which is why it is decoupled.

### Why XML over JSON?

LLMs are much faster and more reliable at streaming XML/Markdown tags than streaming structured JSON. If a JSON string
breaks mid-stream, your app crashes trying to parse it. With XML tags, you can easily parse text as it arrives
line-by-line.

---

## 17 OpenRouter.ai

**OpenRouter.ai** is essentially a "universal adapter" for AI models. It provides a single, unified API that allows you
to access dozens of different Large Language Models (LLMs) from various providers—like OpenAI (GPT-4), Anthropic (
Claude), Google (Gemini), and Meta (Llama)—using just one API key and one account.

### Key Benefits for Application Development

* **Write Once, Swap Anywhere:** Instead of writing completely different code bases to communicate with OpenAI's API,
  Anthropic's API, and others, you write your code once using OpenRouter's API (which adheres to the standard OpenAI
  payload structure). Switching from GPT-4 to Claude is as seamless as changing a single model name string in your
  request.
* **Unified Billing:** Eliminates the overhead of managing credit cards across 10+ different AI platform dashboards. You
  load credits into a single OpenRouter wallet, and it handles payment distribution across the underlying model
  providers based on usage.
* **Fallback Routing:** If primary API servers (e.g., OpenAI) experience a partial outage or severe latency, OpenRouter
  can be configured to dynamically route your prompt traffic to alternative fallbacks like Anthropic, ensuring high
  availability for your production application.
* **Spring AI Integration:** Because OpenRouter mimics the OpenAI API schema, you can configure the native Spring AI
  OpenAI Chat Client directly in your `application.properties` to target OpenRouter's base URL and endpoint instead.
  This instantly hooks up your Spring Boot server to a massive universe of open and closed-source models.
* url - https://openrouter.ai/workspaces

---

## 19 Open Source Reference: AI System Prompts Repository

The GitHub repository `x1xhlol/system-prompts-and-models-of-ai-tools` is a highly popular, curated collection of "
leaked" or extracted system prompts and internal tool configurations from over 30 mainstream AI tools (including
Lovable, v0, Cursor, Devin AI, and Perplexity).

### What is a System Prompt?

A system prompt acts as the hidden instructions or the "rulebook" given to an AI model before the user ever interacts
with it. It dictates the AI's core behavior, tone, guardrails, formatting rules, and exactly how it is allowed to use
external tools.

### Key Features of the Repository

* **Extensive Coverage:** It contains the full text of system prompts for major IDE agents, autonomous builders, and
  search tools.
* **Technical Depth:** Beyond just text prompts, it includes JSON tool-definition files and schemas that reveal exactly
  how these applications structure their workflows and integrate with specific models.
* **Version History:** You can track commit history to see how vendors adjust and iterate their system prompts over time
  to fix AI hallucinations or improve performance.

### How to Use It for Development

* **Improve Prompt Engineering:** Instead of reinventing the wheel, study these advanced prompts. You can borrow proven
  techniques, such as forcing models to "think step-by-step," defining hyper-specific expert personas, or demanding
  rigidly structured XML/JSON outputs.
* **Understand AI Behavior:** If you ever wonder why a specific tool formats its output in a certain way or refuses a
  request, analyzing its system prompt opens up the "black box" and explains the underlying logic.
* **Reference for Product Design:** If you are building your own AI features (like your Lovable clone), these prompts
  provide battle-tested, real-world patterns for context management and tool schemas that have already been scaled to
  millions of users.
* **Security Awareness:** The repository highlights that exposed prompts and configurations are a vulnerability. It
  serves as a reminder to ensure your own internal tools and system instructions are architected securely.

---

## 19 Storage Architecture: PostgreSQL vs. Minio

Choosing between PostgreSQL and Minio depends on the structural shape of the data and the required operational access
patterns.

### The Core Difference

* **PostgreSQL (Relational Database):** Designed for highly structured, transactional data organized into tables with
  explicit schemas and relationships. Optimized for rapid queries, indexing, and executing complex joins.
* **Minio (Object Storage):** An S3-compatible object store built for storing unstructured binary large objects (BLOBs)
  like images, videos, build artifacts, or raw source code files. Accessible entirely over HTTP REST endpoints.

### Side-by-Side Comparison

| Architectural Attribute   | PostgreSQL                                        | Minio                                                                                                        |
|:--------------------------|:--------------------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| **Storage Paradigm**      | Structured rows, tables, foreign keys, JSONB.     | Flat architecture using Buckets and Key-Value lookups.                                                       |
| **Primary Protocol**      | SQL over native binary protocol (Port `5432`).    | HTTP REST APIs (`GET`, `PUT`, `DELETE` over Port `9000`).                                                    |
| **Transaction Integrity** | Strict ACID compliance for data consistency.      | Eventual consistency patterns across distributed clusters.                                                   |
| **Latency & Speed**       | Extremely fast sub-millisecond data manipulation. | Higher latency per request due to HTTP overhead, but optimized for high-throughput streaming of large files. |
| **Data Mutation**         | In-place updates (mutates existing records).      | Immutable writes (updating a file replaces the object or creates a new version).                             |

### System Decoupling Strategy

In production AI applications, these two storage engines work cooperatively to prevent performance degradation:

1. **PostgreSQL holds the Metadata:** It manages application state, user configurations, access control lists, prompt
   logs, and the structural **File Tree** (the paths, filenames, and IDs).
2. **Minio holds the Raw Content:** It stores the heavy, unstructured file text, raw code buffers, and visual assets.

> **Performance Tip:** Never store large text files or raw binaries inside PostgreSQL columns (`text` or `BYTEA`). This
> causes database bloat, degrades RAM page caching, and slows down database backups. Instead, write the file to Minio,
> retrieve its unique URL or storage key, and save that string reference inside your PostgreSQL database record.

In this we are not going to use vector store as if we store our files in vector store which llm would semantically
search upon, llm might get too much or irrelevant data leading to hallucination.

---

## 20 Spring AI Advisors (ChatClient Interceptors)

The `ChatClient` in Spring AI acts as a fluent API wrapper for interacting with Large Language Models (LLMs). A powerful
feature of this client is the concept of **Advisors**.

### What are Advisors?

Advisors act as **middleware or interceptors** for your AI requests. They allow you to apply reusable logic or "
cross-cutting concerns" to your prompt *before* it is sent to the LLM, and to the response *after* it returns.

Instead of scattering custom logic across your application to handle repetitive tasks (like tracking chat history,
counting tokens, or enforcing security), you configure an Advisor once and attach it directly to the `ChatClient`.

*(Note: Under the hood, Advisors implement the `CallAdvisor` or `StreamAdvisor` interfaces, bringing Aspect-Oriented
Programming (AOP) principles to the AI call path).*

### Common Built-in Advisors

Spring AI provides several out-of-the-box advisors to solve common AI engineering problems:

1. **`MessageChatMemoryAdvisor` (Conversational Memory)**

* **Problem:** LLMs are stateless by default. If you ask "What is the capital of France?" and follow up with "What is
  its population?", the LLM will not know what "its" refers to.
* **Solution:** This advisor intercepts the request, retrieves previous conversation history from a `ChatMemory` store (
  like an in-memory window or a PostgreSQL database), and dynamically injects those past messages into the current
  prompt so the AI has full context.

2. **`VectorStoreChatMemoryAdvisor` (Retrieval Augmented Generation - RAG)**

* **Problem:** You want the AI to answer questions based on a massive database of custom documents, but you cannot fit
  millions of documents into a single prompt's context window.
* **Solution:** When a user asks a question, this advisor automatically searches a Vector Database for relevant document
  chunks and seamlessly injects them into the prompt's system instructions before the LLM sees it.

3. **`TokenUsageAdvisor` & `SimpleLoggerAdvisor` (Observability)**

* **Solution:** These intercept the request/response lifecycle to log the raw JSON payloads being sent to the AI
  provider, or to track exactly how many tokens were consumed for billing and observability purposes.

4. **`SafeGuardAdvisor` (Security)**

* **Solution:** Allows you to define guardrails. The advisor intercepts the user prompt, checks if it violates safety
  rules, and can block the request from ever reaching the LLM (returning a predefined safe response instead) saving on
  API costs and preventing misuse.

### Implementation Example

You typically register advisors when building your `ChatClient` using the `.defaultAdvisors()` method. Every prompt sent
from that client will automatically pass through the defined advisor chain.

@Service
public class ChatAssistant {

    private final ChatClient chatClient;

    public ChatAssistant(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem("You are a helpful customer support agent.")
                // Automatically injects chat history into every prompt
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    public String chat(String userMessage) {
        return this.chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

---

## 21 Understanding StringBuilder

To understand `StringBuilder`, you must understand that standard `String` objects in Java are **immutable** (they cannot
be changed once created).

### The Immutability Problem

If you concatenate strings in a loop using the `+` operator, Java does not modify the original string. Instead, it
creates a brand new `String` object in memory for every single iteration and marks the old ones for Garbage Collection.
For large operations, this causes massive memory bloat and CPU spikes.

### The StringBuilder Solution

`StringBuilder` is a mutable sequence of characters. It acts as an adjustable buffer (like a whiteboard). When you call
`.append()`, it modifies the *existing* object in memory rather than creating a new one.

**Rule of Thumb:**

* Use standard `String` (and `+`) for simple, one-off concatenations (e.g., `String name = first + " " + last;`). The
  Java compiler automatically optimizes this anyway.
* **Always** use `StringBuilder` when concatenating strings inside a `for` or `while` loop, or when buffering streamed
  network data (like chunks of text arriving from an LLM).

---

## 22 Project Reactor: Schedulers & Thread Management

When transitioning from JavaScript/RxJS to Java's Project Reactor (Spring WebFlux), the biggest paradigm shift is
managing **Threads**. Unlike JavaScript's single-threaded Event Loop, Java requires you to explicitly assign work to
different thread pools to prevent your server from crashing.

### The Golden Rule of WebFlux

Spring WebFlux runs on a very small pool of extremely fast threads (Netty threads). Their only job is to handle incoming
HTTP network traffic. **You must never block these threads.** If you force a Netty thread to wait for a slow database
query or a file upload, your entire server will freeze and stop accepting new user requests.

### What is a Scheduler?

A `Scheduler` in Project Reactor is a **Thread Manager**. It is the mechanism you use to move heavy, slow, or blocking
tasks off the main fast threads and onto background worker threads.

### Understanding `Schedulers.boundedElastic()`

Reactor provides several types of schedulers, but `boundedElastic()` is specifically engineered for **blocking I/O tasks
** (like saving to PostgreSQL, writing to Minio, or calling slow external APIs).

* **Elastic:** It dynamically creates new background worker threads as the workload increases, and destroys them when
  they sit idle.
* **Bounded:** It has a safety limit. If thousands of database tasks hit at once, it queues them up rather than creating
  infinite threads and exhausting your server's RAM.

### Code Implementation Example

When streaming an AI response, appending chunks to a `StringBuilder` is fast enough for the main thread. However, saving
the final result to the database is slow.

```java
// ... inside your Flux stream
.doOnNext(response -> {
    // FAST: Main thread appends text instantly
    String content = response.getResult().getOutput().getText();
    fullResponseBuffer.append(content);
})
.doOnComplete(() -> {
    // SLOW: Database/File storage operations
    // We hand this off to the background thread pool so the main thread can escape
    Schedulers.boundedElastic().schedule(() -> {
        parseAndSaveFiles(fullResponseBuffer.toString(), projectId);
    });
})