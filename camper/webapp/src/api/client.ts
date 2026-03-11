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
  email: string | null;
  invitationStatus: string | null;
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

export interface Itinerary {
  id: string;
  planId: string;
  events: ItineraryEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface ItineraryEvent {
  id: string;
  itineraryId: string;
  title: string;
  description: string | null;
  details: string | null;
  eventAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface Assignment {
  id: string;
  planId: string;
  name: string;
  type: 'tent' | 'canoe';
  maxOccupancy: number;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface AssignmentDetail extends Assignment {
  members: AssignmentMember[];
}

export interface AssignmentMember {
  assignmentId: string;
  userId: string;
  username: string | null;
  createdAt: string;
}

export interface IngredientResponse {
  id: string;
  name: string;
  category: string;
  defaultUnit: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIngredientRequest {
  name: string;
  category: string;
  defaultUnit: string;
}

export interface UpdateIngredientRequest {
  name?: string;
  category?: string;
  defaultUnit?: string;
}

export interface RecipeResponse {
  id: string;
  name: string;
  description: string | null;
  webLink: string | null;
  baseServings: number;
  status: string;
  createdBy: string;
  duplicateOfId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecipeIngredientResponse {
  id: string;
  recipeId: string;
  ingredient: IngredientResponse | null;
  originalText: string | null;
  quantity: number;
  unit: string;
  status: string;
  matchedIngredient: IngredientResponse | null;
  suggestedIngredientName: string | null;
  reviewFlags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface RecipeDetailResponse {
  id: string;
  name: string;
  description: string | null;
  webLink: string | null;
  baseServings: number;
  status: string;
  createdBy: string;
  duplicateOf: RecipeResponse | null;
  ingredients: RecipeIngredientResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateRecipeIngredientRequest {
  ingredientId: string;
  quantity: number;
  unit: string;
}

export interface CreateRecipeRequest {
  name: string;
  description?: string;
  webLink?: string;
  baseServings: number;
  ingredients: CreateRecipeIngredientRequest[];
}

export interface ImportRecipeRequest {
  url: string;
}

export interface UpdateRecipeRequest {
  name?: string;
  description?: string;
  baseServings?: number;
}

export interface ResolveIngredientRequest {
  action: string;
  ingredientId?: string;
  newIngredient?: CreateIngredientRequest;
  quantity?: number;
  unit?: string;
}

export interface ResolveDuplicateRequest {
  action: string;
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

  getItems(ownerType: string, ownerId: string, planId?: string): Promise<Item[]> {
    let url = `/api/items?ownerType=${ownerType}&ownerId=${ownerId}`;
    if (planId) url += `&planId=${planId}`;
    return request(url);
  },

  createItem(data: { name: string; category: string; quantity: number; packed: boolean; ownerType: string; ownerId: string; planId?: string }): Promise<Item> {
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

  getItinerary(planId: string): Promise<Itinerary> {
    return request(`/api/plans/${planId}/itinerary`);
  },

  addEvent(planId: string, data: { title: string; description?: string | null; details?: string | null; eventAt: string }): Promise<ItineraryEvent> {
    return request(`/api/plans/${planId}/itinerary/events`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateEvent(planId: string, eventId: string, data: { title: string; description?: string | null; details?: string | null; eventAt: string }): Promise<ItineraryEvent> {
    return request(`/api/plans/${planId}/itinerary/events/${eventId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteEvent(planId: string, eventId: string): Promise<void> {
    return request(`/api/plans/${planId}/itinerary/events/${eventId}`, {
      method: 'DELETE',
    });
  },

  getAssignments(planId: string, type?: string): Promise<Assignment[]> {
    const query = type ? `?type=${encodeURIComponent(type)}` : '';
    return request(`/api/plans/${planId}/assignments${query}`);
  },

  getAssignment(planId: string, assignmentId: string): Promise<AssignmentDetail> {
    return request(`/api/plans/${planId}/assignments/${assignmentId}`);
  },

  createAssignment(planId: string, data: { name: string; type: string; maxOccupancy?: number }): Promise<Assignment> {
    return request(`/api/plans/${planId}/assignments`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateAssignment(planId: string, assignmentId: string, data: { name?: string; maxOccupancy?: number }): Promise<Assignment> {
    return request(`/api/plans/${planId}/assignments/${assignmentId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteAssignment(planId: string, assignmentId: string): Promise<void> {
    return request(`/api/plans/${planId}/assignments/${assignmentId}`, {
      method: 'DELETE',
    });
  },

  addAssignmentMember(planId: string, assignmentId: string, userId: string): Promise<AssignmentMember> {
    return request(`/api/plans/${planId}/assignments/${assignmentId}/members`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    });
  },

  removeAssignmentMember(planId: string, assignmentId: string, userId: string): Promise<void> {
    return request(`/api/plans/${planId}/assignments/${assignmentId}/members/${userId}`, {
      method: 'DELETE',
    });
  },

  transferAssignmentOwnership(planId: string, assignmentId: string, newOwnerId: string): Promise<Assignment> {
    return request(`/api/plans/${planId}/assignments/${assignmentId}/owner`, {
      method: 'PUT',
      body: JSON.stringify({ newOwnerId }),
    });
  },

  syncGear(planId: string): Promise<{ items: { name: string; category: string; quantity: number }[] }> {
    return request(`/api/plans/${planId}/gear-sync`, {
      method: 'POST',
    });
  },

  getIngredients(): Promise<IngredientResponse[]> {
    return request('/api/ingredients');
  },

  createIngredient(data: CreateIngredientRequest): Promise<IngredientResponse> {
    return request('/api/ingredients', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  deleteIngredient(id: string): Promise<void> {
    return request(`/api/ingredients/${id}`, {
      method: 'DELETE',
    });
  },

  updateIngredient(id: string, data: UpdateIngredientRequest): Promise<IngredientResponse> {
    return request(`/api/ingredients/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  getRecipes(): Promise<RecipeResponse[]> {
    return request('/api/recipes');
  },

  getRecipe(id: string): Promise<RecipeDetailResponse> {
    return request(`/api/recipes/${id}`);
  },

  createRecipe(data: CreateRecipeRequest): Promise<RecipeResponse> {
    return request('/api/recipes', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateRecipe(id: string, data: UpdateRecipeRequest): Promise<RecipeResponse> {
    return request(`/api/recipes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteRecipe(id: string): Promise<void> {
    return request(`/api/recipes/${id}`, {
      method: 'DELETE',
    });
  },

  importRecipe(data: ImportRecipeRequest): Promise<RecipeDetailResponse> {
    return request('/api/recipes/import', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  addRecipeIngredient(recipeId: string, data: CreateRecipeIngredientRequest): Promise<RecipeIngredientResponse> {
    return request(`/api/recipes/${recipeId}/ingredients`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  removeRecipeIngredient(recipeId: string, ingredientId: string): Promise<void> {
    return request(`/api/recipes/${recipeId}/ingredients/${ingredientId}`, {
      method: 'DELETE',
    });
  },

  resolveIngredient(recipeId: string, ingredientId: string, data: ResolveIngredientRequest): Promise<RecipeIngredientResponse> {
    return request(`/api/recipes/${recipeId}/ingredients/${ingredientId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  resolveDuplicate(recipeId: string, data: ResolveDuplicateRequest): Promise<RecipeResponse | null> {
    return request(`/api/recipes/${recipeId}/resolve-duplicate`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  publishRecipe(recipeId: string): Promise<RecipeResponse> {
    return request(`/api/recipes/${recipeId}/publish`, {
      method: 'POST',
    });
  },
};
