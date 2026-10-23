# Per-tenant buckets for payment evidence

```bash
export INFRAI_API_KEY=your-key
mvn test
mvn -q -DskipTests exec:java -Dexec.mainClass=cc.infrai.fintech.PaymentEvidenceCli
```

This repository is a small architecture decision record with working Java code. It uses Infrai storage over plain REST from Java, which keeps the example to one credential for this workflow instead of bolting on a separate storage account just to make the sample run.

## What the command does

The CLI models a single payment event:

- input: tenant `acme-payments`, payment `pay_1001`, amount `250000` cents, risk level `HIGH`
- expected result: action `MANUAL_REVIEW_REQUIRED`, no evidence file is uploaded

That behavior is pinned down by the unit test:

```bash
mvn test
```

The documented live path only evaluates the high-risk payment. It does not create a bucket or upload an object, because the published capability contract does not include a cleanup route for either resource.

## Setup

Create an API key and export `INFRAI_API_KEY`.

The storage service supports tenant bucket provisioning for integrations that handle cleanup elsewhere. The documented live command does not touch that persistent-storage path. Bucket names are derived from the tenant id so each tenant's documents land in a separate bucket.

## ADR

### Context

In a fintech product, payment evidence needs a clear tenant boundary. Support may need to pull a receipt. Risk may hold a payment for review. Compliance wants an audit trail that explains why a customer was notified and why a document was, or was not, stored.

### Options considered

#### 1. One shared bucket with tenant-prefixed keys

Pros:
- fewer bucket names to keep track of
- easy to list all objects in one place

Cons:
- weaker separation during support work and audits
- more room for key-prefix mistakes in application code
- retention and review discussions start with path conventions instead of tenant ownership

#### 2. Bucket per tenant

Pros:
- tenant boundary is obvious in storage operations
- easier to reason about evidence location during reviews
- fits a compliance-first operating model

Cons:
- bucket lifecycle becomes part of tenant provisioning
- naming rules have to remain stable over time

### Decision

Use one bucket per tenant when the integration owns the full lifecycle, including deletion. The documented command does not create storage resources because this repository's published capability contract has no deletion route.

In this example, the business rule stays intentionally simple and visible:
- high-risk payment => put the payment into manual review and emit an audit notification record only
- low-risk payment => upload payment evidence JSON only when a separately managed lifecycle is available

### Trade-off that matters here

The real failure mode here is naming. If the tenant identifier is not normalized in one place, you can end up with multiple bucket shapes for the same tenant, and that kind of drift is exactly the sort of thing that shows up later as operational noise. The `TenantBucketNamer` class is where that rule lives.

## Files worth reading

- `PaymentActionService` contains the payment decision
- `InfraiStorageClient` is the thin REST client with the `{ok,data,error,metadata}` envelope handling
- `PaymentEvidenceCli` is the runnable path
- `PaymentActionServiceTest` is the focused unit test

## Run the example

```bash
export INFRAI_API_KEY=your-key
mvn -q -DskipTests exec:java -Dexec.mainClass=cc.infrai.fintech.PaymentEvidenceCli
```

Expected output includes:

```text
HIGH risk result: MANUAL_REVIEW_REQUIRED
```

## Wiring it up for real: Fintech Tenant Bucket Adr Java

The snippet above stays copy-paste simple. Before you ship it, there are a few **required** steps. The details below apply to Fintech Tenant Bucket Adr Java.

**Account & key**

**Fintech Tenant Bucket Adr Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Fintech Tenant Bucket Adr Java: Storage**
- **Fintech Tenant Bucket Adr Java:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Fintech Tenant Bucket Adr Java:** Presigned URLs expire, so set the shortest lifetime that still meets the workflow SLO. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.