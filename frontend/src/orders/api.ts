export type OrderItemRequest = {
  productId: number;
  quantity: number;
};

export type PlaceOrderRequest = {
  items: OrderItemRequest[];
};

export type OrderItemResponse = {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type OrderStatus = "PENDING" | "PAID" | "FAILED";

export type OrderResponse = {
  id: number;
  status: OrderStatus;
  items: OrderItemResponse[];
  total: number;
  createdAt: string;
};

export async function placeOrder(
  accessToken: string,
  request: PlaceOrderRequest,
): Promise<OrderResponse> {
  const response = await fetch("/api/orders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error(`Failed to place order (status=${response.status})`);
  }

  return await response.json();
}

export async function fetchOrder(
  accessToken: string,
  id: number,
  signal: AbortSignal,
): Promise<OrderResponse> {
  const response = await fetch(`/api/orders/${id}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    signal,
  });
  if (!response.ok) {
    throw new Error(
      `Failed to fetch order with id ${id} (status=${response.status})`,
    );
  }

  return await response.json();
}
