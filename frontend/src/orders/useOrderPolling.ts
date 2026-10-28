import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { fetchOrder, type OrderResponse, type OrderStatus } from "./api";

export const textForStatus: Record<OrderStatus, string> = {
  PENDING: "Processing payment...",
  PAID: "Paid",
  FAILED: "Payment declined",
};

/**
 * Keeps a copy of the given order and, while its status is PENDING, polls the
 * backend once per second until the payment result arrives. Polling stops when
 * the component unmounts or the user is logged out.
 *
 * @param order the order as first received; later changes to this argument are
 *   ignored, because the hook keeps its own copy
 * @returns the latest known version of the order
 */
export function useOrderPolling(order: OrderResponse): OrderResponse {
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

  return currentOrder;
}
