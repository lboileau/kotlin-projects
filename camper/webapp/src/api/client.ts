export interface User {
  id: string;
  email: string;
  username: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Plan {
  id: string;
  name: string;
  visibility: string;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  isMember: boolean;
}

export interface PlanMember {
  planId: string;
  userId: string;
  username: string | null;
  createdAt: string;
}

export interface Item {
  id: string;
  planId: string | null;
  userId: string | null;
  name: string;
  category: string;
  quantity: number;
  packed: boolean;
  createdAt: string;
  updatedAt: string;
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  const userId = localStorage.getItem('userId');
  if (userId) {
    headers['X-User-Id'] = userId;
  }

  const response = await fetch(path, { ...options, headers });

  if (response.status === 204) {
    return undefined as T;
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Request failed: ${response.status}`);
  }

  return response.json();
}

export const api = {
  login(email: string): Promise<User> {
    return request('/api/auth', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  register(email: string, username?: string): Promise<User> {
    return request('/api/users', {
      method: 'POST',
      body: JSON.stringify({ email, username: username || undefined }),
    });
  },

  updateUser(userId: string, username: string): Promise<User> {
    return request(`/api/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({ username }),
    });
  },

  getPlans(): Promise<Plan[]> {
    return request('/api/plans');
  },

  createPlan(name: string): Promise<Plan> {
    return request('/api/plans', {
      method: 'POST',
      body: JSON.stringify({ name }),
    });
  },

  getPlanMembers(planId: string): Promise<PlanMember[]> {
    return request(`/api/plans/${planId}/members`);
  },

  addMember(planId: string, email: string): Promise<PlanMember> {
    return request(`/api/plans/${planId}/members`, {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  removeMember(planId: string, memberId: string): Promise<void> {
    return request(`/api/plans/${planId}/members/${memberId}`, {
      method: 'DELETE',
    });
  },

  updatePlan(planId: string, data: { name: string; visibility?: string }): Promise<Plan> {
    return request(`/api/plans/${planId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deletePlan(planId: string): Promise<void> {
    return request(`/api/plans/${planId}`, {
      method: 'DELETE',
    });
  },

  getItems(ownerType: string, ownerId: string): Promise<Item[]> {
    return request(`/api/items?ownerType=${ownerType}&ownerId=${ownerId}`);
  },

  createItem(data: { name: string; category: string; quantity: number; packed: boolean; ownerType: string; ownerId: string }): Promise<Item> {
    return request('/api/items', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateItem(itemId: string, data: { name: string; category: string; quantity: number; packed: boolean }): Promise<Item> {
    return request(`/api/items/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteItem(itemId: string): Promise<void> {
    return request(`/api/items/${itemId}`, {
      method: 'DELETE',
    });
  },
};
