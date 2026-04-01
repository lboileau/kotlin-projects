export interface AvatarResponse {
  hairStyle: string;
  hairColor: string;
  skinColor: string;
  clothingStyle: string;
  pantsColor: string;
  shirtColor: string;
}

export interface User {
  id: string;
  email: string;
  username: string | null;
  experienceLevel: string | null;
  avatarSeed: string | null;
  profileCompleted: boolean;
  dietaryRestrictions: string[];
  avatar: AvatarResponse | null;
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
  invitedBy: string | null;
  role: string;
  avatarSeed: string | null;
  avatar: AvatarResponse | null;
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

export interface LinkResponse {
  id: string;
  url: string;
  label: string | null;
  createdAt: string;
}

export interface LinkInput {
  url: string;
  label: string | null;
}

export interface Itinerary {
  id: string;
  planId: string;
  events: ItineraryEvent[];
  totalEstimatedCost: number | null;
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
  category: string;
  estimatedCost: number | null;
  location: string | null;
  eventEndAt: string | null;
  links: LinkResponse[];
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
  meal: string | null;
  theme: string | null;
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
  suggestedCategory: string | null;
  suggestedUnit: string | null;
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
  meal: string | null;
  theme: string | null;
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
  meal?: string;
  theme?: string;
  ingredients: CreateRecipeIngredientRequest[];
}

export interface ImportRecipeRequest {
  url: string;
}

export interface UpdateRecipeRequest {
  name?: string;
  description?: string;
  baseServings?: number;
  meal?: string;
  theme?: string;
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

// ── Meal Plan Types ──────────────────────────

export interface MealPlanResponse {
  id: string;
  planId: string | null;
  name: string;
  servings: number;
  scalingMode: string;
  isTemplate: boolean;
  sourceTemplateId: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface MealPlanDetailResponse {
  id: string;
  planId: string | null;
  name: string;
  servings: number;
  scalingMode: string;
  isTemplate: boolean;
  sourceTemplateId: string | null;
  createdBy: string;
  days: MealPlanDayResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface MealPlanDayResponse {
  id: string;
  dayNumber: number;
  meals: MealsByTypeResponse;
}

export interface MealsByTypeResponse {
  breakfast: MealPlanRecipeDetailResponse[];
  lunch: MealPlanRecipeDetailResponse[];
  dinner: MealPlanRecipeDetailResponse[];
  snack: MealPlanRecipeDetailResponse[];
}

export interface MealPlanRecipeDetailResponse {
  id: string;
  recipeId: string;
  recipeName: string;
  baseServings: number;
  scaleFactor: number;
  isFullyPurchased: boolean;
  ingredients: MealPlanIngredientResponse[];
}

export interface MealPlanIngredientResponse {
  recipeIngredientId: string;
  ingredientId: string;
  ingredientName: string;
  category: string;
  quantity: number;
  scaledQuantity: number;
  unit: string;
}

export interface ShoppingListResponse {
  mealPlanId: string;
  servings: number;
  scalingMode: string;
  totalItems: number;
  fullyPurchasedCount: number;
  categories: ShoppingListCategoryResponse[];
}

export interface ShoppingListCategoryResponse {
  category: string;
  items: ShoppingListItemResponse[];
}

export interface ShoppingListItemResponse {
  ingredientId: string;
  ingredientName: string;
  quantityRequired: number;
  quantityPurchased: number;
  unit: string;
  status: string;
  usedInRecipes: string[];
}

export interface CreateMealPlanRequest {
  name: string;
  servings: number;
  scalingMode?: string;
  isTemplate?: boolean;
  planId?: string;
}

export interface UpdateMealPlanRequest {
  name?: string;
  servings?: number;
  scalingMode?: string;
}

export interface AddRecipeToMealRequest {
  mealType: string;
  recipeId: string;
}

export interface UpdatePurchaseRequest {
  ingredientId: string;
  unit: string;
  quantityPurchased: number;
}

export interface SaveAsTemplateRequest {
  name: string;
}

export interface CopyToTripRequest {
  planId: string;
  servings?: number;
}

// ── Log Book Types ──────────────────────────

export interface LogBookFaqResponse {
  id: string;
  planId: string;
  question: string;
  askedById: string;
  answer: string | null;
  answeredById: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LogBookJournalEntryResponse {
  id: string;
  planId: string;
  userId: string;
  pageNumber: number;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface AskFaqRequest {
  question: string;
}

export interface AnswerFaqRequest {
  answer: string;
}

export interface CreateJournalEntryRequest {
  content: string;
}

export interface UpdateJournalEntryRequest {
  content: string;
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

  updateUser(userId: string, data: {
    username: string;
    experienceLevel?: string | null;
    dietaryRestrictions?: string[] | null;
    profileCompleted?: boolean;
    avatarSeed?: string;
  }): Promise<User> {
    return request(`/api/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  randomizeAvatar(userId: string): Promise<{ seed: string; avatar: AvatarResponse }> {
    return request(`/api/users/${userId}/randomize-avatar`, {
      method: 'POST',
    });
  },

  getAvatar(userId: string): Promise<AvatarResponse> {
    return request(`/api/users/${userId}/avatar`);
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

  updateMemberRole(planId: string, userId: string, role: string): Promise<PlanMember> {
    return request(`/api/plans/${planId}/members/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
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

  addEvent(planId: string, data: {
    title: string;
    description?: string | null;
    details?: string | null;
    eventAt: string;
    category: string;
    estimatedCost?: number | null;
    location?: string | null;
    eventEndAt?: string | null;
    links?: LinkInput[] | null;
  }): Promise<ItineraryEvent> {
    return request(`/api/plans/${planId}/itinerary/events`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateEvent(planId: string, eventId: string, data: {
    title: string;
    description?: string | null;
    details?: string | null;
    eventAt: string;
    category: string;
    estimatedCost?: number | null;
    location?: string | null;
    eventEndAt?: string | null;
    links?: LinkInput[] | null;
  }): Promise<ItineraryEvent> {
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

  // ── Meal Plans ──────────────────────────

  getMealPlanForTrip(planId: string): Promise<MealPlanDetailResponse | null> {
    return request(`/api/meal-plans?planId=${planId}`);
  },

  getMealPlanDetail(id: string): Promise<MealPlanDetailResponse> {
    return request(`/api/meal-plans/${id}`);
  },

  createMealPlan(data: CreateMealPlanRequest): Promise<MealPlanResponse> {
    return request('/api/meal-plans', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateMealPlan(id: string, data: UpdateMealPlanRequest): Promise<MealPlanResponse> {
    return request(`/api/meal-plans/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteMealPlan(id: string): Promise<void> {
    return request(`/api/meal-plans/${id}`, {
      method: 'DELETE',
    });
  },

  addMealPlanDay(mealPlanId: string, dayNumber: number): Promise<MealPlanDayResponse> {
    return request(`/api/meal-plans/${mealPlanId}/days`, {
      method: 'POST',
      body: JSON.stringify({ dayNumber }),
    });
  },

  removeMealPlanDay(mealPlanId: string, dayId: string): Promise<void> {
    return request(`/api/meal-plans/${mealPlanId}/days/${dayId}`, {
      method: 'DELETE',
    });
  },

  addRecipeToMeal(mealPlanId: string, dayId: string, data: AddRecipeToMealRequest): Promise<MealPlanRecipeDetailResponse> {
    return request(`/api/meal-plans/${mealPlanId}/days/${dayId}/recipes`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  removeRecipeFromMeal(mealPlanRecipeId: string): Promise<void> {
    return request(`/api/meal-plan-recipes/${mealPlanRecipeId}`, {
      method: 'DELETE',
    });
  },

  getShoppingList(mealPlanId: string): Promise<ShoppingListResponse> {
    return request(`/api/meal-plans/${mealPlanId}/shopping-list`);
  },

  updatePurchase(mealPlanId: string, data: UpdatePurchaseRequest): Promise<void> {
    return request(`/api/meal-plans/${mealPlanId}/shopping-list`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },

  resetPurchases(mealPlanId: string): Promise<void> {
    return request(`/api/meal-plans/${mealPlanId}/shopping-list`, {
      method: 'DELETE',
    });
  },

  // ── Templates ──────────────────────────

  getTemplates(): Promise<MealPlanResponse[]> {
    return request('/api/meal-plans/templates');
  },

  saveAsTemplate(mealPlanId: string, data: SaveAsTemplateRequest): Promise<MealPlanResponse> {
    return request(`/api/meal-plans/${mealPlanId}/save-as-template`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  copyTemplateToTrip(templateId: string, data: CopyToTripRequest): Promise<MealPlanDetailResponse> {
    return request(`/api/meal-plans/${templateId}/copy-to-trip`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // ── Log Book ──────────────────────────

  getLogBookFaqs(planId: string): Promise<LogBookFaqResponse[]> {
    return request(`/api/plans/${planId}/log-book/faqs`);
  },

  createLogBookFaq(planId: string, data: AskFaqRequest): Promise<LogBookFaqResponse> {
    return request(`/api/plans/${planId}/log-book/faqs`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  answerLogBookFaq(planId: string, faqId: string, data: AnswerFaqRequest): Promise<LogBookFaqResponse> {
    return request(`/api/plans/${planId}/log-book/faqs/${faqId}/answer`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteLogBookFaq(planId: string, faqId: string): Promise<void> {
    return request(`/api/plans/${planId}/log-book/faqs/${faqId}`, {
      method: 'DELETE',
    });
  },

  getLogBookJournalEntries(planId: string): Promise<LogBookJournalEntryResponse[]> {
    return request(`/api/plans/${planId}/log-book/journal`);
  },

  createLogBookJournalEntry(planId: string, data: CreateJournalEntryRequest): Promise<LogBookJournalEntryResponse> {
    return request(`/api/plans/${planId}/log-book/journal`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateLogBookJournalEntry(planId: string, entryId: string, data: UpdateJournalEntryRequest): Promise<LogBookJournalEntryResponse> {
    return request(`/api/plans/${planId}/log-book/journal/${entryId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteLogBookJournalEntry(planId: string, entryId: string): Promise<void> {
    return request(`/api/plans/${planId}/log-book/journal/${entryId}`, {
      method: 'DELETE',
    });
  },
};
