// 로그인 상태는 이 탭의 메모리(변수)에만 둔다. localStorage·sessionStorage는 쓰지 않는다. // 수정 (FE-01)
let session = { accessToken: "", user: null };
export const getSession = () => session;
export function clearSession() {
  session = { accessToken: "", user: null };
}

// 서버 오류 code → 화면에 보여 줄 안내 문구 // 수정 (FE-03)
const ERROR_MESSAGES = { // 수정 (FE-03)
  INVALID_CREDENTIALS: "이메일 또는 비밀번호가 올바르지 않습니다.", // 수정 (FE-03)
  TOKEN_REQUIRED: "로그인이 필요합니다. 로그인한 뒤 다시 시도하세요.", // 수정 (FE-03)
  INVALID_TOKEN: "로그인 정보가 올바르지 않습니다. 다시 로그인하세요.", // 수정 (FE-03)
  TOKEN_EXPIRED: "로그인 시간이 만료되었습니다. 다시 로그인하세요.", // 수정 (FE-03)
  FORBIDDEN: "본인이 작성한 글만 수정·삭제할 수 있습니다.", // 수정 (FE-03)
  NOT_FOUND: "해당 게시글이 없습니다. 이미 삭제되었을 수 있습니다.", // 수정 (FE-03)
}; // 수정 (FE-03)

function authorizationHeaders() {
  // 토큰이 있을 때만 헤더를 만든다. 없으면 "Bearer undefined" 같은 헤더를 보내지 않는다. // 수정 (FE-02)
  if (!session.accessToken) return {}; // 수정 (FE-02)
  return { Authorization: `Bearer ${session.accessToken}` }; // 수정 (FE-02)
}

export async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeaders(),
      ...options.headers,
    },
  });
  // 204는 본문이 없으므로 JSON을 읽지 않는다. 그 밖에도 본문이 JSON이 아니면 null로 둔다. // 수정 (FE-03)
  let data = null; // 수정 (FE-03)
  if (response.status !== 204) { // 수정 (FE-03)
    const text = await response.text(); // 수정 (FE-03)
    try { // 수정 (FE-03)
      data = text ? JSON.parse(text) : null; // 수정 (FE-03)
    } catch { // 수정 (FE-03)
      data = null; // 수정 (FE-03)
    } // 수정 (FE-03)
  } // 수정 (FE-03)
  if (!response.ok) {
    const code = data?.code; // 수정 (FE-03)
    // 401이면 메모리의 토큰과 사용자를 지워 로그인 상태를 해제한다. 403은 해제하지 않는다. // 수정 (FE-03)
    if (response.status === 401) clearSession(); // 수정 (FE-03)
    let message = ERROR_MESSAGES[code]; // 수정 (FE-03)
    if (!message && response.status === 400) // 수정 (FE-03)
      message = `입력을 확인하세요. (${data?.message || "형식 오류"})`; // 수정 (FE-03)
    const error = new Error(
        message || data?.message || code || `요청에 실패했습니다. (HTTP ${response.status})`, // 수정 (FE-03)
    );
    error.status = response.status;
    error.code = code; // 수정 (FE-03)
    throw error;
  }
  return data;
}

export async function login(email, password) {
  // [삭제] throw new Error("로그인 연결은 학생 구현 항목입니다. FE-01을 완성하세요.");
  clearSession(); // 수정 (FE-01) 이전 로그인 상태를 비우고 새로 로그인
  const data = await request("/auth/login", { // 수정 (FE-01) email·password를 JSON으로 전송
    method: "POST", // 수정 (FE-01)
    body: JSON.stringify({ email, password }), // 수정 (FE-01)
  }); // 수정 (FE-01)
  session = { accessToken: data.accessToken, user: data.user }; // 수정 (FE-01) 성공했을 때만 메모리에 저장
  return data.user; // 수정 (FE-01)
}

export function canEdit(post) {
  // [삭제] return true;
  // 로그인한 사용자의 id와 글의 authorId가 같을 때만 수정·삭제 버튼을 보여 준다. // 수정 (FE-04)
  // 버튼을 숨기는 건 화면 편의일 뿐이고, 실제 차단은 서버(assertOwner)가 한다. // 수정 (FE-04)
  return session.user != null && post != null && post.authorId === session.user.id; // 수정 (FE-04)
}

export const api = {
  list: () => request("/posts"),
  detail: (id) => request(`/posts/${id}`),
  create: (input) =>
      request("/posts", { method: "POST", body: JSON.stringify(input) }),
  update: (id, input) =>
      request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id) => request(`/posts/${id}`, { method: "DELETE" }),
  me: () => request("/me"),
};