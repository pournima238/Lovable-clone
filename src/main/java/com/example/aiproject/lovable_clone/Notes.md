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

```
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
```

---

## 23 LLM Prefix Caching (Prompt Caching)

Prefix caching (often called **Prompt Caching**) is a system-level optimization used by Large Language Model (LLM)
providers to reduce latency, compute costs, and energy consumption. It works by temporarily storing the expensive
computational states of text that appears at the very beginning (the prefix) of multiple prompts.

### The "Re-reading the Book" Analogy

Imagine you are asked to answer specific questions about a 500-page manual.

* **Without Caching:** Every time you are asked a new question, you have to read the entire 500-page manual from page 1,
  plus the new question, before you can start answering.
* **With Caching:** You read the 500-page manual once and keep the knowledge fresh in your short-term memory. When a new
  question arrives, you only read the new question. You can start answering immediately.

### How It Works Technically: The KV Cache

To understand prefix caching, you need to understand how LLMs process text:

1. **The Prefill Phase:** When you send a prompt, the LLM reads all the input tokens simultaneously. The model's
   Attention mechanism calculates complex mathematical vectors called **Keys (K)** and **Values (V)** for every single
   token to understand the context. This phase requires massive parallel GPU computation.
2. **The KV Cache:** Once calculated, these Key and Value vectors are temporarily stored in the GPU's memory (VRAM).
   This is the "KV Cache."
3. **The Optimization:** Normally, the KV Cache is wiped clean after your response is generated. With Prefix Caching,
   the server intentionally retains the KV Cache for the beginning sequence of your tokens (the prefix). If your next
   prompt starts with the **exact same tokens**, the LLM skips the heavy "Prefill" phase for that section, loads the
   cache from RAM, and only calculates the KV vectors for the new, unique tokens at the end.

### Key Benefits

* **Reduced Time-To-First-Token (TTFT):** Because the model skips reading the heavy system instructions or document
  context, it starts streaming the response back to your frontend much faster.
* **Cost Efficiency:** Processing cached tokens requires significantly less computational power. Many API providers (
  like Anthropic or OpenAI) charge substantially less (often 50% less) for cached input tokens compared to uncached
  ones.

### Common Use Cases

Prefix caching is highly effective whenever a large block of text remains static across multiple requests:

* **Massive System Prompts:** Prepending a huge rulebook or coding style guide to every user query.
* **Retrieval-Augmented Generation (RAG):** Passing a large, static PDF into the context window and asking the user to
  chat with it. The PDF is cached; only the chat messages change.
* **Multi-Turn Chat:** As a conversation grows, the earlier messages become a static prefix, making long conversations
  cheaper to maintain.

### The Golden Rules of Caching

1. **Strict Exact Matching:** The cache only works if the prefix is *exactly* the same, character for character.
   Changing a single comma or whitespace at the very beginning of the prompt invalidates the cache for everything that
   follows it.
2. **Order Matters:** The cached text must appear at the absolute beginning of the prompt.

* ✅ **Cached:** `[Huge Static Codebase] + "Update the Profile button."`
* ❌ **Not Cached:** `"Update the Profile button." + [Huge Static Codebase]` *(The dynamic part broke the exact match at
  the start).*

3. **Cache Eviction:** Because GPU memory is limited, providers typically evict (delete) caches that haven't been used
   recently (usually within 5 to 60 minutes).

For this purpose we have used file Tree advisor and have created augment function in which we make sure our system
prompt is always at first then user message and after that the file tree.

---

### 24 AI Coding Assistant Flow: File Reading

**User:** "make the app dark theme"
<br>↓

**FileTreeContextAdvisor** injects the file list into the system prompt
<br>↓

**AI** sees `FILE_TREE`, decides it needs to read `src/App.tsx`
<br>↓

**AI** calls `read_files(["src/App.tsx"])`
<br>↓

**CodeGenerationTools** fetches content from MinIO
<br>↓

**Content** is injected back into the conversation
<br>↓

**AI** now generates the correct updated file

## 25 Code Generation Flow — Lovable Clone

### Overview

The system lets users send a chat message like *"make a task manager app"* and the backend streams AI-generated React
code back, saves the files to MinIO, and tracks them in PostgreSQL.

---

### Entry Point — `ChatController.java`

```
POST /api/chat/stream
```

Receives `{ message, projectId }`, creates an `SseEmitter` (10 min timeout), subscribes to the reactive stream from
`AiGenerationService`, and pushes each chunk to the client as Server-Sent Events. Returns the emitter immediately while
streaming continues in the background.

> **Important:** We use `spring-boot-starter-webmvc` (servlet stack), NOT WebFlux. This means we must use `SseEmitter`
> and manually call `.subscribe()` on the Flux. Returning `Flux<ServerSentEvent>` directly only works with WebFlux. This
> was a key bug — the Flux was being built but never subscribed to, causing instant empty responses.

---

### Core Service — `AiGenerationServiceImpl.java`

The heart of the system. Key things it does:

**1. Builds the prompt:**

```java
Flux.defer(() ->{
        return chatClient.

prompt()
        .

system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
        .

user(userMessage)
        .

tools(codeGenerationTools)        // gives AI ability to read files
        .

advisors(fileTreeContextAdvisor)  // injects file tree into context
        .

advisors(advisorSpec ->advisorSpec.

params(advisorParams)) // passes projectId/userId
        .

stream()
        .

content();
})
```

> **Why `Flux.defer()`?** It ensures the entire chain is built and executed fresh on each subscription. Required for
> correct behavior in the WebMVC + reactive streams combination.

**2. Buffers the full response using `AtomicReference<StringBuilder>`:**

```java
AtomicReference<StringBuilder> bufferRef = new AtomicReference<>(new StringBuilder());
// each chunk appended in doOnNext()
```

> **Why `AtomicReference`?** `doOnNext` can fire from different threads in a reactive pipeline. `AtomicReference`
> ensures thread-safe access to the buffer.

**3. On completion, parses and saves files:**

```java
// Regex extracts file content from AI response
Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>",Pattern.DOTALL)

// saves each file to MinIO + PostgreSQL
parseAndSaveFiles(fullResponse, projectId);
```

---

### System Prompt — `PromptUtils.java`

Tells the AI:

- Use **only** React 18 + TypeScript + Vite + Tailwind CSS 4 + daisyUI v5 + lucide-react. Never import antd, @mui, or
  anything else.
- Respond in strict XML format using `<message>`, `<tool>`, `<file>` tags
- Always read existing files via `<tool>` before editing them
- Never hallucinate file contents — always read first
- Dark theme via `data-theme="dark"` on root element
- Max 100 lines per file, no TODOs, no hardcoded colors

> **Important:** Do NOT use `LocalDateTime.now()` inside the static prompt field. Since it is evaluated at class load
> time, it never updates and wastes tokens. It was also a cause of unnecessarily long prompts.

---

### File Tree Advisor — `FileTreeContextAdvisor.java`

Implements `StreamAdvisor` with `getOrder() = -1` (runs before `SimpleLoggerAdvisor` which defaults to 0).

**What it does:** Before every AI call, fetches all files for the project from PostgreSQL and injects them into the
system prompt:

```
---- FILE_TREE ----
[FileNode(path=src/App.tsx), FileNode(path=src/main.tsx), ...]
```

This tells the AI what files exist so it can decide which ones to read/modify.

**How it works:**

```java
// Strips system messages, adds file tree as new SystemMessage, re-adds user messages
return request.mutate()
    .

prompt(new Prompt(allMessages, request.prompt().

getOptions()))
        .

build();
```

**Critical ordering rule for prompt caching:**
The advisor always assembles messages in this order:

1. Original system message (static — gets prefix cached)
2. File tree system message
3. User messages (dynamic)

This ensures the large static system prompt is always at the prefix position, making it eligible for LLM prefix caching.

> **Important Spring AI 2.0.0 Bug:** Passing both `.advisors(myAdvisor)` and `.params()` inside the same lambda silently
> drops the advisor. Always split them into two separate `.advisors()` calls:
> ```java
> .advisors(fileTreeContextAdvisor)          // register advisor
> .advisors(advisorSpec -> advisorSpec.params(advisorParams))  // pass params
> ```

---

### File Reading Tool — `CodeGenerationTools.java`

Not a Spring `@Component` — instantiated manually per request with the specific `projectId`:

```java
CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectFileService, projectId);
```

The `@Tool` annotated method `readFiles(List<String> paths)` is called by the AI when it outputs
`<tool args="src/App.tsx">`. Spring AI intercepts this, calls the method, fetches file content from MinIO, and injects
the result back into the conversation so the AI can read actual file contents before generating code.

**Required dependency in `pom.xml` (not pulled transitively — must be added manually):**

```xml

<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-jackson</artifactId>
    <version>4.36.0</version>
</dependency>
<dependency>
<groupId>com.github.victools</groupId>
<artifactId>jsonschema-generator</artifactId>
<version>4.36.0</version>
</dependency>
```

> Spring AI 2.0.0 uses `victools` to convert `@Tool` annotated methods into JSON schemas for the LLM. Without this
> dependency you get `NoClassDefFoundError: JacksonSchemaModule` and the entire stream fails silently.

---

### File Storage — `ProjectFileServiceImpl.java`

Two key operations:

- **Save:** Uploads content to MinIO at `projects/{projectId}/{filePath}` and upserts a `ProjectFile` record in
  PostgreSQL with the path and MinIO object key.
- **Get file tree:** Queries PostgreSQL for all `ProjectFile` records for a project, maps them to `FileNode` objects —
  this is what `FileTreeContextAdvisor` uses.
- **Get content:** Downloads file bytes from MinIO by object key and returns as string — this is what
  `CodeGenerationTools.readFiles()` calls.

---

### Project Template — `ProjectTemplateServiceImpl.java`

Called when a new project is created. Copies all files from:

```
starter-projects/react-starter/
```

to:

```
projects/{projectId}/
```

Using MinIO server-side copy (fast — no download/upload needed), then saves `ProjectFile` records to PostgreSQL so the
file tree is immediately populated for the AI.

The starter template contains: `index.html`, `package.json`, `vite.config.ts`, `tsconfig.json`, `tsconfig.node.json`,
`src/main.tsx`, `src/App.tsx`, `src/index.css`, `src/vite-env.d.ts`.

---

### Config — `AiConfig.java`

```java
.defaultOptions(OpenAiChatOptions.builder()
    .

toolChoice("auto"))  // ← CRITICAL
```

| Value        | Behavior                                                                 |
|--------------|--------------------------------------------------------------------------|
| `"none"`     | Model is blocked from calling any tools — causes instant empty responses |
| `"auto"`     | Model decides when to call tools (correct)                               |
| `"required"` | Model must always call a tool                                            |

> `toolChoice("none")` was the root cause of the empty response bug. Always use `"auto"` when tools are registered.

**LLM Provider:** Groq (`https://api.groq.com/openai/v1`) with model `llama-3.3-70b-versatile`. Groq's free tier gives
14,400 req/day with no stream timeouts, making it far more reliable than OpenRouter free models for streaming.

---

### Key Dependencies in `pom.xml`

```xml
spring-ai-starter-model-openai              <!-- ChatClient, streaming -->
        spring-ai-starter-vector-store-pgvector     <!-- PgVector (future RAG use) -->
        jsonschema-module-jackson                   <!-- REQUIRED for @Tool registration -->
        jsonschema-generator                        <!-- REQUIRED for @Tool registration -->
        minio                                       <!-- file storage -->
```

---

### Complete Request Flow

```
POST /api/chat/stream
        ↓
ChatController → creates SseEmitter, calls .subscribe() on Flux
        ↓
Flux.defer() builds the chain fresh on subscription
        ↓
FileTreeContextAdvisor (order=-1) → fetches all project files from PostgreSQL
                                  → injects FILE_TREE into system prompt
        ↓
ChatClient sends augmented request to Groq
        ↓
AI sees FILE_TREE, decides to read src/App.tsx
AI outputs: <tool args="src/App.tsx">
        ↓
Spring AI intercepts tool call → CodeGenerationTools.readFiles()
        ↓
File content fetched from MinIO, injected back into conversation
        ↓
AI generates: <file path="src/App.tsx">...updated code...</file>
        ↓
Chunks streamed back → SseEmitter.send() → client sees real-time output
        ↓
doOnComplete() → Schedulers.boundedElastic() → parseAndSaveFiles()
        ↓
Regex extracts <file> tags → saved to MinIO + PostgreSQL
```

---

### Common Bugs & Fixes Encountered

| Bug                                         | Root Cause                                                    | Fix                                                            |
|---------------------------------------------|---------------------------------------------------------------|----------------------------------------------------------------|
| Empty response (88-99ms)                    | `toolChoice("none")` blocking all tool calls                  | Change to `toolChoice("auto")`                                 |
| `NoClassDefFoundError: JacksonSchemaModule` | Missing `victools` dependency                                 | Add `jsonschema-module-jackson` to pom.xml                     |
| Advisor not being applied                   | Both advisor + params inside same lambda                      | Split into two separate `.advisors()` calls                    |
| `Flux` never subscribed                     | Returning `Flux` directly in WebMVC (needs WebFlux)           | Use `SseEmitter` + `.subscribe()` in controller                |
| Circular bean creation error                | `@PostConstruct` calling `minioClient()` method on same class | Extract bucket init to separate `StorageInitializer` component |
| Stream timeout mid-response                 | OpenRouter free model rate limits                             | Switched to Groq free tier                                     |
| Model hallucinating imports (antd, @mui)    | System prompt not explicitly banning external libraries       | Added strict stack list to system prompt                       |

## 26 The N+1 Problem

### The Simple Explanation

The N+1 problem is when your code makes **1 query to get a list**, then makes **N more queries** (one for each item in
that list) to get related data — when it could have fetched everything in **just 1 query**.

---

### Real World Analogy

Imagine you are a teacher and you want to know **which city every student in your class lives in**.

**The N+1 Way (Bad):**

1. You ask the class: *"Can everyone write their name on the board?"* → **1 question**
2. Then you call each student one by one: *"John, where do you live?"* → *"Sarah, where do you live?"* → *"Mike, where
   do you live?"* ...

If you have 30 students, you asked **31 questions total (1 + 30)**.

**The Smart Way (Good):**

1. You just ask once: *"Can everyone write their name AND city on the board?"* → **1 question, done.**

---

### Code Example

Say you have `Project` and `ProjectMember` — one project has many members.

**The N+1 Way:**

```java
// Query 1: fetch all projects
List<Project> projects = projectRepository.findAll();

for(
Project project :projects){
// Query 2, 3, 4... N+1: fetch members for EACH project separately
List<ProjectMember> members = memberRepository.findByProjectId(project.getId());
    System.out.

println(project.getName() +" has "+members.

size() +" members");
        }
```

If you have 50 projects, this fires **51 queries** to the database:

```sql
SELECT *
FROM projects; -- 1 query
SELECT *
FROM project_members
WHERE project_id = 1; -- query for project 1
SELECT *
FROM project_members
WHERE project_id = 2; -- query for project 2
SELECT *
FROM project_members
WHERE project_id = 3;
-- query for project 3
-- ... 47 more queries
```

**The Fix — JOIN everything in 1 query:**

```java
// Using JPA with JOIN FETCH
@Query("SELECT p FROM Project p JOIN FETCH p.members")
List<Project> findAllWithMembers();
```

This fires just **1 query:**

```sql
SELECT p.*, pm.*
FROM projects p
         JOIN project_members pm ON pm.project_id = p.id;
```

---

### Why It's Dangerous

It's sneaky because it **works fine locally** with 5 projects. But in production with 10,000 projects, your server
suddenly fires **10,001 database queries per request** — your database gets overwhelmed and your API slows to a crawl.

---

### How to Spot It

In your project you have `show-sql: true` in `application.yaml`. If you ever see the **same query repeating in a loop**
in your logs with different IDs, that's the N+1 problem:

```sql
SELECT *
FROM project_members
WHERE project_id = 1
SELECT *
FROM project_members
WHERE project_id = 2
SELECT *
FROM project_members
WHERE project_id = 3
-- this repeating is the red flag
```

---

### The Fixes in JPA/Spring Boot

| Fix                                 | When to Use                                             |
|-------------------------------------|---------------------------------------------------------|
| `JOIN FETCH` in JPQL                | When you always need the related data                   |
| `@EntityGraph` on repository method | Cleaner alternative to JOIN FETCH                       |
| `@BatchSize(size = 25)`             | Loads related entities in batches instead of one by one |
| DTO projections with a single query | Best for read-heavy endpoints                           |

The most common fix you'll use is `JOIN FETCH`:

```java

@Query("SELECT p FROM Project p JOIN FETCH p.members WHERE p.id = :id")
Optional<Project> findByIdWithMembers(@Param("id") Long id);
```

---

```markdown
## 27 Chat Persistence Flow — Saving Messages & Events

### Overview

After the AI finishes streaming its response, the backend persists the full conversation in the background using
`Schedulers.boundedElastic()` so the main thread is never blocked.

---

### Entity Hierarchy

```

ChatSession (projectId + userId — composite key)
└── ChatMessage (USER or ASSISTANT role)
└── ChatEvent (THOUGHT | MESSAGE | FILE_EDIT | TOOL_LOG)

```

- One `ChatSession` per user per project.
- One `ChatMessage` per turn (user sends one, assistant sends one).
- One `ChatMessage` has many `ChatEvent`s — the structured breakdown of what the AI actually did.

---

### Why ChatEvents Instead of Raw Text?

The AI responds in structured XML:

```xml
<message phase="planning">I will update App.tsx...</message>
<tool args="src/App.tsx">Reading file...</tool>
<file path="src/App.tsx">...full file content...</file>
<message phase="completed">Done!</message>
```

Saving this as a raw string is useless for the frontend. Instead, `LlmResponseParser` uses regex to break it into typed
`ChatEvent` records so the frontend can render each part differently (thought bubble, chat bubble, file diff, tool log).

---

### The Complete Save Flow

```
doOnComplete() fires
    ↓
Schedulers.boundedElastic().schedule()   ← background thread, never blocks Netty
    ↓
finalizeChats(userMessage, chatSession, fullText, duration, usage)
    ↓
┌─────────────────────────────────────────────────────┐
│ 1. Record token usage → UsageLog (daily counter)    │
│ 2. Save USER ChatMessage (with promptTokens)        │
│ 3. Save ASSISTANT ChatMessage (with completionTokens)│
│ 4. Parse full AI response → List<ChatEvent>         │
│ 5. Prepend THOUGHT event ("Thought for Xs")         │
│ 6. FILE_EDIT events → projectFileService.saveFile() │
│ 7. saveAll(chatEventList) → batch insert            │
└─────────────────────────────────────────────────────┘
```

---

### LlmResponseParser — How Parsing Works

Uses a single regex to match all three XML tag types in one pass:

```java
Pattern.compile("(<(message|file|tool)([^>]*)>)([\\s\\S]*?)(</\\2>)")
```

| Tag                 | Maps To                   | Extra Fields                      |
|---------------------|---------------------------|-----------------------------------|
| `<message>`         | `ChatEventType.MESSAGE`   | content = markdown text           |
| `<file path="...">` | `ChatEventType.FILE_EDIT` | filePath extracted from attribute |
| `<tool args="...">` | `ChatEventType.TOOL_LOG`  | args stored in metadata           |

A `THOUGHT` event is always prepended manually with `sequenceOrder = 0` and content `"Thought for Xs"` where X is the
time between request sent and first token received.

---

### ChatSession — Created Lazily

```
streamResponse() called
    ↓
createChatSessionIfNotExists(projectId, userId)
    ↓
Lookup by composite key (ChatSessionId)
    ├── Found → reuse existing session
    └── Not found → create new ChatSession and save
```

This means the first message to a project creates the session; all subsequent messages reuse it.

---

### Fetching Chat History

`GET /api/chat/projects/{projectId}` → `ChatServiceImpl.getProjectChatHistory()`

Uses a single `JOIN FETCH` query to avoid the N+1 problem:

```java
@Query("""
        SELECT DISTINCT m FROM ChatMessage m
        LEFT JOIN FETCH m.events e
        WHERE m.chatSession = :chatSession
        ORDER BY m.createdAt ASC, e.sequenceOrder ASC
        """)
```

This fetches all messages **and** their events in one query, ordered correctly for rendering.

---

### Key Design Decisions

| Decision                                    | Reason                                                                           |
|---------------------------------------------|----------------------------------------------------------------------------------|
| Save on `doOnComplete`, not during stream   | Can't write to DB mid-stream; full text needed for regex parsing                 |
| `boundedElastic()` for DB writes            | DB calls are blocking I/O — must never run on Netty threads                      |
| Batch `saveAll()` for events                | One INSERT per event would be N+1 writes; batch is one round trip                |
| THOUGHT event prepended in code, not parsed | AI never outputs a `<thought>` tag; duration is calculated server-side           |
| FILE_EDIT triggers file save                | Parser and file persistence are coupled here intentionally — one source of truth |

### Null-Result Chunks During Tool Calls

When the AI calls a tool (e.g. `read_files`), Spring AI emits intermediate `ChatResponse` chunks where `getResult()`
returns `null`. These are internal bookkeeping chunks carrying tool-call metadata, not actual text.

**Always filter these out before your processing logic:**

```java
.stream()
.

chatResponse()
.

filter(response ->response !=null
        &&response.

getResult() !=null
        &&response.

getResult().

getOutput() !=null)  // ← drops tool-call meta-chunks
        .

doOnNext(response ->{
// safe — getResult() is guaranteed non-null here
String content = response.getResult().getOutput().getText();
    fullResponseBuffer.

append(content !=null?content:"");
})
```

Without this filter, placing `getResult().getOutput().getText()` before a null check causes an immediate
`NullPointerException` and crashes the stream.
``
---

## 28 Code Execution System Architecture — Live Preview

![img_3.png](img_3.png)

```
┌─────────────┐        /deploy/36         ┌─────────────────┐
│  Frontend   │ ───────────────────────→  │  Spring Backend │
│             │ ←─── returns preview URL ─ │  (orchestrator) │
└─────────────┘                           └────────┬────────┘
       │                                           │ fabric8
       │ opens URL                                 ↓
       │                                  ┌─────────────────┐
       │                                  │   Kubernetes    │
       │                                  │   Pod 36        │
       ↓                                  │  runner+syncer  │
┌─────────────┐   Redis lookup            └────────┬────────┘
│   Reverse   │ ──────────────→ 192.244.1.16:5173  │
│   Proxy     │ ←────────────────────────────────── │
└─────────────┘
```

### The Problem

In the Lovable clone, the user chats with the AI, the AI generates React code, and the user needs to see a
**live preview** of that React app running in real time.

You can't run it in the browser directly (no Node.js). You can't run all projects on one shared server (security
nightmare). So you need **one isolated environment per project** that runs `npm run dev` and serves the Vite preview.
That's exactly what this architecture solves.

---

### The Complete Flow

```
User clicks Preview on Project 36
        ↓
POST /deploy/36 → Spring Backend
        ↓
Fetch code files from MinIO
        ↓
fabric8 K8s client creates Pod 36
    ├── runner container → npm run dev → Vite server on :5173
    └── syncer container → watches MinIO for file changes
        ↓
Pod 36 gets IP: 192.244.1.16:5173
        ↓
Spring Backend writes to Redis:
    project-36.app.domain.com → 192.244.1.16:5173
        ↓
User visits project-36.app.domain.com
        ↓
Reverse Proxy reads Redis → forwards to 192.244.1.16:5173
        ↓
User sees live React app
        ↓
AI edits file → saved to MinIO → syncer picks up → HMR → preview updates
```

---

### Step-by-Step Breakdown

**Step 1 — Frontend calls Spring Backend**

```
Frontend → POST /deploy/36
```

The `36` is the `projectId`. Spring Backend receives this and finds or spins up a running environment for project 36.

---

**Step 2 — Spring Backend fetches code from MinIO**

All AI-generated files are already saved in MinIO at:

```
projects/36/src/App.tsx
projects/36/src/index.css
projects/36/package.json
```

MinIO also pulls npm dependencies from the npm registry so the container has everything it needs.

---

**Step 3 — Kubernetes creates a Pod for Project 36**

Spring Backend uses the **fabric8 Kubernetes client** (a Java library) to programmatically create the pod:

```java
k8sClient.runCommand("npm install","pod36","runner");
```

Each pod has **two containers**:

- **runner** — runs `npm run dev`, starts Vite dev server on `:5173`, enables HMR
- **syncer** — watches MinIO for file changes; when AI saves a new file, syncer triggers HMR so the preview
  updates live without a refresh

Pod 36 gets its own internal IP:

```
192.244.1.16:5173
```

---

**Step 4 — Redis stores the routing mapping**

Once the pod is running, Spring Backend saves this into Redis:

```
project-36.app.yourdomain.com → 192.244.1.16:5173
```

Redis is used here because it is extremely fast. Every single preview page load hits this lookup — a PostgreSQL
query would be too slow.

---

**Step 5 — User visits the preview URL**

The Reverse Proxy (Nginx or Traefik) catches all `*.app.yourdomain.com` traffic, reads Redis:

```
project-36 → 192.244.1.16:5173
```

And forwards the request directly to that pod. The user sees their live React app.

---

**Step 6 — AI edits a file, preview updates live**

```
User: "make the button red"
        ↓
AI generates new App.tsx
        ↓
Spring Backend saves it to MinIO
        ↓
Syncer container in Pod 36 detects the change
        ↓
Triggers HMR → browser updates without refresh
        ↓
User sees the red button instantly
```

---

### Security — Kubernetes Network Policy

Each pod is completely isolated from every other pod:

```
Pod 36 (User A's project)  →  CANNOT talk to  →  Pod 37 (User B's project)
```

This is enforced via Kubernetes Network Policy. Without it, a malicious user could potentially access another
user's running code or internal pod data.

---

### Key Design Decisions

| Decision                           | Reason                                                                 |
|------------------------------------|------------------------------------------------------------------------|
| One pod per project                | Complete isolation — one project can't crash or access another         |
| Vite dev server inside pod         | HMR gives instant preview updates without full rebuilds                |
| Redis for routing                  | Sub-millisecond lookup — far faster than a DB query on every page load |
| fabric8 K8s client                 | Spring-native way to programmatically create and manage pods           |
| Network policy blocking pod-to-pod | Security — prevents cross-tenant data leakage                          |
| Syncer sidecar container           | Decouples file watching from the runner — single responsibility        |

---

### How This Maps to What You've Already Built

| What you built                     | What this architecture adds              |
|------------------------------------|------------------------------------------|
| AI generates code → saved to MinIO | Syncer reads from that same MinIO        |
| `projectId` in your DB             | Becomes the pod name (`pod36`)           |
| Spring Backend                     | Gets a new `DeployService` using fabric8 |
| Subdomain setup                    | Reverse proxy + Redis routing layer      |
| PostgreSQL stores file metadata    | Redis stores the live IP routing table   |

The part already built (AI chat → code generation → MinIO storage) is the **write path**.
This architecture is the **read/execution path** — taking those saved files and actually running them for the user to
see.
---

## 29 kind (Kubernetes IN Docker) — Local Kubernetes for Development

### What is kind?

`kind` is a tool that runs a full **Kubernetes cluster locally inside Docker containers** on your laptop.
It is free, lightweight, and requires no cloud provider or VM setup.

```
Without kind → deploy to AWS/GCP → costs money + slow feedback
With kind    → deploy to local cluster → free + instant
```

Your laptop runs Docker, Docker runs kind, kind runs Kubernetes, Kubernetes runs your pods.

```
Your Laptop
└── Docker
    └── kind cluster (itself a Docker container)
        └── Kubernetes running inside
            ├── Pod 36 (runner + syncer)
            ├── Pod 37 (runner + syncer)
            └── Pod 38 (runner + syncer)
```

---

### Installation on Windows (PowerShell as Administrator)

**Step 1 — Install Docker Desktop**

- Download from https://www.docker.com/products/docker-desktop
- Install and make sure Docker is running (whale icon in taskbar)

**Step 2 — Install kubectl**

```powershell
winget install Kubernetes.kubectl
```

Verify (restart shell first if needed):

```powershell
kubectl version --client
# Expected: Client Version: v1.x.x
```

**Step 3 — Install kind**

```powershell
winget install Kubernetes.kind
```

> After install, **close and reopen PowerShell** — the PATH needs to refresh.

Verify:

```powershell
kind version
# Expected: kind v0.32.0 go1.xx.x windows/amd64
```

---

### Essential Commands

```powershell
# Create a cluster
kind create cluster --name lovable-dev

# Verify cluster is running
kubectl get nodes

# Load your local Docker image into kind
# (kind cannot see local Docker images by default)
kind load docker-image lovable-runner:latest --name lovable-dev

# Delete a cluster
kind delete cluster --name lovable-dev

# See all running pods
kubectl get pods

# See logs of a specific container inside a pod
kubectl logs pod-36 -c runner

# Delete a specific pod
kubectl delete pod pod-36

# Point kubectl to your kind cluster (if it loses context)
kind export kubeconfig --name lovable-dev
```

---

### kind vs Other Tools

| Tool          | Where it runs               | Best for                              |
|---------------|-----------------------------|---------------------------------------|
| `kind`        | Docker containers on laptop | CI pipelines, local dev, fast startup |
| `minikube`    | VM on laptop                | Local dev, heavier but more features  |
| AWS EKS / GKE | Cloud VMs                   | Production                            |

---

### Common Issues on Windows

| Issue                                   | Fix                                                           |
|-----------------------------------------|---------------------------------------------------------------|
| `kind: command not found` after install | Close and reopen PowerShell — PATH needs to refresh           |
| Docker not running                      | Open Docker Desktop and wait for whale icon to stop animating |
| `kubectl` can't connect to cluster      | Run `kind export kubeconfig --name lovable-dev`               |
| WSL2 error on Docker start              | Enable WSL2 in Windows Features → Virtual Machine Platform    |
| Commands not working                    | Always run PowerShell as Administrator                        |

---

## 30 Proxy vs Reverse Proxy vs API Gateway

### Proxy (Forward Proxy)

Sits in front of the **client**. The client sends requests through it so the
destination server never knows the real client's identity.

```
Client → Proxy → Server
Server only sees Proxy, never the real Client
```

Use cases: VPNs, corporate firewalls blocking certain websites, client anonymity.

---

### Reverse Proxy

Sits in front of the **server**. The client talks to it and it silently
forwards to the right backend server. Client never knows which real server
handled the request.

```
Client → Reverse Proxy → Server A
                       → Server B
                       → Server C
Client only sees the Reverse Proxy address
```

Use cases: load balancing, SSL termination, routing preview URLs to pods
(exactly what your Lovable clone uses for `project-36.app.domain.com → pod IP`).

Tools: Nginx, Traefik, HAProxy

---

### API Gateway

A smart reverse proxy with extra features built in. Does everything a reverse
proxy does plus authentication, rate limiting, logging, and request transformation.

```
Client → API Gateway → verify JWT
                     → check rate limit
                     → log request
                     → route to correct microservice
```

Use cases: microservices architecture, centralizing auth and rate limiting
outside of your business logic.

Tools: Kong, AWS API Gateway, Spring Cloud Gateway

---

### Difference

|                  | Proxy                   | Reverse Proxy            | API Gateway                              |
|------------------|-------------------------|--------------------------|------------------------------------------|
| Sits in front of | Client                  | Server                   | Server                                   |
| Hides            | Client identity         | Server identity          | Server identity                          |
| Main purpose     | Anonymity / filtering   | Routing / load balancing | Routing + Auth + Rate limiting + Logging |
| Intelligence     | Dumb                    | Semi-smart               | Very smart                               |
| Tools            | VPN, corporate firewall | Nginx, Traefik           | Kong, AWS API Gateway                    |

---

### Relationship

```
Proxy ⊂ Reverse Proxy ⊂ API Gateway

Every API Gateway is a Reverse Proxy
A Proxy and Reverse Proxy are opposites (client-side vs server-side)
```

---

### In Your Lovable Clone

```
project-36.app.domain.com
        ↓
Reverse Proxy (Nginx)     ← reads Redis, routes to correct pod
        ↓
Pod 36 :5173


/api/chat/stream
        ↓
Spring Backend            ← currently handles auth + rate limiting itself
(could add API Gateway here to extract those concerns out of Spring)
```

---

## 31 Understanding runner.yml file

Great question! Let me break this file down piece by piece.

---

## What This File Is

This is a **Deployment** YAML — it tells Kubernetes to create and manage a pool of pre-warmed runner pods so you don't
have to wait for cold starts when a user clicks preview.

---

## Top Level

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: runner-pool
  namespace: shuttle-apps
```

- `apiVersion: apps/v1` — which Kubernetes API version handles this resource
- `kind: Deployment` — not a raw Pod, a Deployment. Deployment manages pods for you — if a pod crashes, Deployment
  automatically restarts it
- `name: runner-pool` — the name of this deployment
- `namespace: shuttle-apps` — all pods created by this deployment live inside the `shuttle-apps` namespace

---

## The Clever Part — Pre-warming with replicas

```yaml
spec:
  replicas: 2
  selector:
    matchLabels:
      app: runner
      status: idle
  template:
    metadata:
      labels:
        app: runner
        status: idle
```

This is the **pool strategy**. Instead of creating a pod when a user clicks preview (slow — 30-60 seconds for npm
install), this creates **2 idle pods upfront** that are already warm and waiting.

```
Cluster always has:
├── runner-pod-1  (status: idle) ← waiting for a user
└── runner-pod-2  (status: idle) ← waiting for a user

User clicks preview on Project 36:
├── runner-pod-1  (status: running) ← assigned to project 36
└── runner-pod-2  (status: idle)   ← still waiting

Spring Backend creates a new idle pod to refill the pool:
├── runner-pod-1  (status: running) ← project 36
├── runner-pod-2  (status: idle)    ← waiting
└── runner-pod-3  (status: idle)    ← newly created to refill
```

The `status: idle` label is what Spring Backend queries to find an available pod:

```java
// fabric8 pseudocode
k8sClient.pods()
    .

inNamespace("shuttle-apps")
    .

withLabel("status","idle")  // find any idle pod
    .

list()
    .

getItems()
    .

get(0);                       // grab the first one
```

Then it changes the label to `status: running` and assigns it to the project.

---

## Volumes — Shared Storage Between Containers

```yaml
volumes:
  - name: workspace
    emptyDir: { }
  - name: pnpm-store
    hostPath:
      path: /mnt/pnpm-store
      type: DirectoryOrCreate
```

Two volumes are defined here:

**workspace (`emptyDir`):**

- An empty folder that exists only while the pod is alive
- Shared between runner and syncer containers
- This is where the project files live (`/app`)
- When syncer downloads files from MinIO, it writes to this volume
- runner reads from this same volume to serve the Vite app

```
┌─────────────── Pod ───────────────┐
│  runner ──→ /app (workspace)      │
│  syncer ──→ /app (workspace)      │
│  both see the same files          │
└───────────────────────────────────┘
```

**pnpm-store (`hostPath`):**

- Points to `/mnt/pnpm-store` on the actual machine (Node) running this pod
- This is a **cache** — all pods on the same machine share one pnpm store
- So if Project 36 installs React, and Project 37 also needs React, it doesn't download it again
- `DirectoryOrCreate` means create the folder if it doesn't exist

```
Machine (Kubernetes Node)
└── /mnt/pnpm-store          ← shared pnpm cache
    ├── react@18.0.0
    ├── vite@5.0.0
    └── tailwindcss@4.0.0

Pod 36 → uses /mnt/pnpm-store → React already cached → fast install
Pod 37 → uses /mnt/pnpm-store → React already cached → fast install
```

---

## Runner Container

```yaml
- name: runner
  image: node:20-alpine
  workingDir: /app
  command: [ "/bin/sh", "-c", "sleep infinity" ]
  volumeMounts:
    - name: workspace
      mountPath: /app
    - name: pnpm-store
      mountPath: /root/.local/share/pnpm
  ports:
    - containerPort: 5173
  resources:
    limits:
      memory: "1Gi"
```

- `image: node:20-alpine` — lightweight Node.js image, no custom image needed
- `workingDir: /app` — all commands run from `/app` by default
- `command: sleep infinity` — **this is the key trick**. The container starts but does nothing. It just stays alive
  waiting. When Spring Backend assigns a project to it, fabric8 executes commands inside this already-running container:

```java
// fabric8 executes commands inside the sleeping container
k8sClient.pods()
    .

inNamespace("shuttle-apps")
    .

withName("runner-pod-1")
    .

inContainer("runner")
    .

exec("pnpm","install");

k8sClient.

pods()
    .

inNamespace("shuttle-apps")
    .

withName("runner-pod-1")
    .

inContainer("runner")
    .

exec("pnpm","run","dev");
```

- `containerPort: 5173` — Vite runs here
- `memory: 1Gi` — hard cap so one user's project can't crash the whole node

---

## Syncer Container

```yaml
- name: syncer
  image: minio/mc
  command: [ "/bin/sh", "-c", "sleep infinity" ]
  env:
    - name: MC_HOST_myminio
      value: "http://minioadmin:minioadmin123@minio-service:9000"
  volumeMounts:
    - name: workspace
      mountPath: /app
```

- `image: minio/mc` — MinIO client image, has all the tools to talk to MinIO
- `sleep infinity` — same trick as runner, just waits
- `MC_HOST_myminio` — pre-configured MinIO connection. `myminio` becomes an alias so commands look like:

```bash
# Instead of:
mc cp http://minioadmin:minioadmin123@minio-service:9000/projects/36/src/App.tsx /app/src/App.tsx

# You just write:
mc cp myminio/projects/36/src/App.tsx /app/src/App.tsx
```

When Spring Backend assigns project 36 to this pod, fabric8 tells the syncer to pull files:

```java
k8sClient.pods()
    .

withName("runner-pod-1")
    .

inContainer("syncer")
    .

exec("mc","cp","--recursive","myminio/projects/36/","/app/");
```

And for live updates (when AI edits a file):

```java
// Watch for changes and sync continuously
k8sClient.pods()
    .

withName("runner-pod-1")
    .

inContainer("syncer")
    .

exec("mc","watch","myminio/projects/36/");
```

---

## The Complete Picture

```
Deployment creates 2 idle pods upfront
        ↓
Each pod has runner (sleeping) + syncer (sleeping)
        ↓
User clicks Preview on Project 36
        ↓
Spring Backend finds idle pod via label selector
        ↓
Changes label: status: idle → status: running
        ↓
fabric8 tells syncer: pull files from MinIO → /app/
        ↓
fabric8 tells runner: pnpm install → pnpm run dev
        ↓
Vite starts on :5173
        ↓
Spring Backend writes to Redis: project-36.domain.com → pod-IP:5173
        ↓
Returns preview URL to Frontend
        ↓
AI edits a file → saved to MinIO → syncer detects → copies to /app/ → HMR updates preview
```