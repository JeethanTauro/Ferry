const API_BASE_URL = "http://localhost:8080";

export async function loginToFerry(idToken) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/auth/login`,
    {
      method: "POST",

      headers: {
        Authorization: `Bearer ${idToken}`,
        "Content-Type": "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error("Failed to authenticate with Ferry");
  }

  return response.json();
}


export async function getEndpoints(idToken) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/endpoints`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error("Failed to fetch endpoints");
  }

  return response.json();
}


export async function createEndpoint(idToken, endpointData) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/endpoints`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${idToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(endpointData),
    }
  );

  if (!response.ok) {
    throw new Error("Failed to create endpoint");
  }

  return response.json();
}

export async function disableEndpoint(idToken, endpointId) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/endpoints/${endpointId}/disable`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error("Failed to disable endpoint");
  }
}

export async function enableEndpoint(idToken, endpointId) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/endpoints/${endpointId}/enable`,
    {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error("Failed to enable endpoint");
  }
}

export async function deleteEndpoint(idToken, endpointId) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/endpoints/${endpointId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error("Failed to delete endpoint");
  }
}

export async function getEndpoint(idToken, endpointId) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/endpoints/${endpointId}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${idToken}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error("Failed to fetch endpoint");
  }

  return response.json();
}