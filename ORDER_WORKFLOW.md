# Order workflow

## Order states

| Current state | Next state | Who or what moves it | Rule |
| --- | --- | --- | --- |
| Pending | Processing | Billplz callback, payment verification, or admin | Payment must be confirmed as paid. |
| Pending | Cancelled | Customer, admin, or expiry job | Payment must still be unpaid. Billplz must confirm the bill can no longer be paid. |
| Processing | Shipped | Admin saves shipment details | Carrier and tracking number are required. |
| Shipped | Completed | Admin | Marks the order delivered. |

`Completed` and `Cancelled` are terminal states. A paid order cannot be cancelled through the normal cancellation flow.

## Payment and inventory

- Checkout creates a `Pending` order and reserves its stock by reducing product quantities.
- A failed or incomplete payment keeps the order `Pending`; the customer can retry while the payment window is open.
- After payment is confirmed, the order becomes `Processing` and its stock remains deducted.
- Cancelling or expiring an unpaid order first asks Billplz to remove the unpaid bill. Stock is restored only after that succeeds.
- If Billplz cannot confirm the bill is unpaid, the order and stock reservation stay in place until payment can be verified safely.
- Shipping and completion do not change inventory.
- Archiving a `Pending` order also cancels it and restores stock using the same unpaid-payment check.

## Who can cancel

- A customer can cancel only their own, active `Pending` order when it is not paid.
- An admin can cancel a `Pending` order under the same Billplz and inventory checks.
- The expiry job applies those checks after the payment deadline.
- `Processing`, `Shipped`, and `Completed` orders cannot be cancelled through this flow.

## Related endpoints

- Customer cancellation: `POST /api/orders/{id}/cancel`
- Payment verification: `POST /api/orders/{id}/payment/reconcile`
- Admin status update: `PATCH /api/orders/{id}/status`
- Admin shipment update: `PATCH /api/orders/{id}/shipment`
