import { useEffect, useState } from "react";
import { fetchOrder, type OrderResponse, type OrderStatus } from "./api";
import { formatPrice } from "../format";
import styles from "./OrderConfirmation.module.css";
import { useAuth } from "react-oidc-context";

const textForStatus: Record<OrderStatus, string> = {
  PENDING: "Processing payment...",
  PAID: "Paid",
  FAILED: "Payment declined",
};

export type OrderConfirmationProps = {
  order: OrderResponse;
};

export function OrderConfirmation({ order }: OrderConfirmationProps) {
  const [currentOrder, setCurrentOrder] = useState<OrderResponse>(order);
  const { user } = useAuth();

  useEffect(() => {
    if (currentOrder.status !== "PENDING") {
      return;
    }

    const controller = new AbortController();
    let pollTimeoutId: number | undefined;

    const poll = async () => {
      try {
        const accessToken = user?.access_token;
        if (!accessToken) {
          return;
        }

        const refreshedOrder = await fetchOrder(
          accessToken,
          currentOrder.id,
          controller.signal,
        );

        // The cleanup may have run while this request was finishing (unmount, or a
        // status change restarting the effect); aborting can't cancel a request
        // that already completed, so stop here instead of scheduling another poll.
        if (controller.signal.aborted) {
          return;
        }

        setCurrentOrder(refreshedOrder);
        if (refreshedOrder.status === "PENDING") {
          pollTimeoutId = setTimeout(poll, 1000);
        }
      } catch (err) {
        if (controller.signal.aborted) {
          return;
        }

        console.error(err);
        pollTimeoutId = setTimeout(poll, 1000);
      }
    };

    pollTimeoutId = setTimeout(poll, 1000);

    return () => {
      clearTimeout(pollTimeoutId);
      controller.abort();
    };
  }, [currentOrder.id, currentOrder.status, user]);

  return (
    <div
      role="status"
      className={styles.confirmation}
      data-status={currentOrder.status}
    >
      <p>Order #{currentOrder.id} placed.</p>
      <p>Status: {textForStatus[currentOrder.status]}</p>
      <p>Total: {formatPrice(currentOrder.total)}</p>
    </div>
  );
}
