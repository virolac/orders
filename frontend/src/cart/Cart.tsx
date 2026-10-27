import { useState } from "react";
import { formatPrice } from "../format";
import { placeOrder, type OrderResponse } from "../orders/api";
import type { CartItem } from "./types";
import { OrderConfirmation } from "../orders/OrderConfirmation";

function calculateEstimatedTotal(items: CartItem[]): number {
  return items.reduce((sum, { product, quantity }) => {
    return sum + product.price * quantity;
  }, 0);
}

type IdleState = {
  status: "idle";
};

type SubmittingState = {
  status: "submitting";
};

type FailedState = {
  status: "failed";
};

type PlacedState = {
  status: "placed";
  order: OrderResponse;
};

type OrderSubmitState = IdleState | SubmittingState | FailedState | PlacedState;

export type CartProps = {
  items: CartItem[];
  onOrderPlaced: () => void;
};

export function Cart({ items, onOrderPlaced }: CartProps) {
  const [orderState, setOrderState] = useState<OrderSubmitState>({
    status: "idle",
  });

  const handlePlaceOrder = async () => {
    setOrderState({ status: "submitting" });

    const orderItems = items.map(({ product, quantity }) => ({
      productId: product.id,
      quantity,
    }));

    try {
      const placedOrder = await placeOrder({ items: orderItems });

      setOrderState({ status: "placed", order: placedOrder });
      onOrderPlaced();
    } catch (err) {
      console.error(err);
      setOrderState({ status: "failed" });
    }
  };

  let cartContents;
  if (items.length > 0) {
    cartContents = (
      <>
        <ul>
          {items.map(({ product, quantity }) => (
            <li key={product.id}>
              <span>{product.name}</span>
              <span>×{quantity}</span>
              <span>{formatPrice(product.price * quantity)}</span>
            </li>
          ))}
        </ul>
        <p>Estimated total: {formatPrice(calculateEstimatedTotal(items))}</p>
        <button
          type="button"
          onClick={handlePlaceOrder}
          disabled={orderState.status === "submitting"}
        >
          {orderState.status === "submitting"
            ? "Placing order..."
            : "Place order"}
        </button>
      </>
    );
  } else {
    cartContents = <p>Your cart is empty</p>;
  }

  return (
    <>
      {orderState.status === "placed" && (
        <OrderConfirmation order={orderState.order} key={orderState.order.id} />
      )}
      {cartContents}
      {orderState.status === "failed" && (
        <p role="alert">Couldn't place your order. Please try again.</p>
      )}
    </>
  );
}
