import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import { api, login, getSession, clearSession, canEdit } from "./api";
import "./style.css";
function App() {
  const [posts, setPosts] = useState([]),
    [selected, setSelected] = useState(null);
  const [title, setTitle] = useState(""),
    [content, setContent] = useState(""),
    [editing, setEditing] = useState(null);
  const [email, setEmail] = useState("alice@class.test"),
    [password, setPassword] = useState("Classroom!2026");
  const [user, setUser] = useState(null),
    [message, setMessage] = useState("게시판을 불러오는 중입니다."),
    [busy, setBusy] = useState(false);
  const refresh = async () => setPosts(await api.list());
  async function run(action, success) {
    setBusy(true);
    try {
      await action();
      setMessage(success);
    } catch (e) {
      setMessage(e.message);
    } finally {
      setUser(getSession().user);
      setBusy(false);
    }
  }
  useEffect(() => {
    run(refresh, "게시판이 준비되었습니다.");
  }, []);
  function reset() {
    setTitle("");
    setContent("");
    setEditing(null);
  }
  async function save(e) {
    e.preventDefault();
    await run(async () => {
      const p = editing
        ? await api.update(editing, { title, content })
        : await api.create({ title, content });
      setSelected(p);
      reset();
      await refresh();
    }, "게시글을 저장했습니다.");
  }
  return (
    <main>
      <header>
        <span className="eyebrow">WEB SERVICE DESIGN · JWT BOARD</span> {/* 수정 */}
        <h1>오늘의 학습 기록</h1> 
        <p className="muted">공부한 내용을 기록하고 서로의 학습 과정을 나누는 게시판입니다.</p> 
      </header>
      <p className="banner">
        {/* 수정 (FE-04) 데모 배너 → 서비스 안내 */}
        누구나 학습 기록을 읽을 수 있고, 로그인한 사용자만 글을 쓸 수 있으며, 본인이 쓴 글만 수정·삭제할 수 있습니다.
        로그인 상태는 이 탭에만 보관되므로 새로고침하면 다시 로그인해야 합니다.
      </p>
      <div role="status" className="message">
        {message}
      </div>
      <div className="grid">
        <aside>
          <section className="card">
            <h2>로그인</h2>
            <p>{user ? `${user.name}님 로그인 중` : "로그아웃 상태"}</p>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                run(() => login(email, password), "로그인했습니다.");
              }}
            >
              <label>
                이메일
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </label>
              <label>
                비밀번호
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </label>
              <div className="actions">
                <button disabled={busy}>로그인</button>
                <button
                  type="button"
                  className="secondary"
                  onClick={() => {
                    clearSession();
                    setUser(null);
                    setMessage("이 탭의 로그인 상태를 해제했습니다.");
                  }}
                >
                  로그아웃
                </button>
              </div>
            </form>
            <p>
              <small>
                테스트 계정: alice@class.test / bob@class.test
                <br />
                공통 비밀번호: Classroom!2026
              </small>
            </p>
            <button
              className="secondary"
              disabled={busy}
              onClick={() =>
                run(async () => {
                  const me = await api.me();
                  setSelected(null);
                  alert(`${me.name} (${me.email})`);
                }, "내 정보 조회를 완료했습니다.")
              }
            >
              내 정보 조회
            </button>
          </section>
          <section className="card">
            <h2>{editing ? "게시글 수정" : "새 글 작성"}</h2>
            <form onSubmit={save}>
              <label>
                제목
                <input
                  maxLength={100}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </label>
              <label>
                내용
                <textarea
                  maxLength={5000}
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  required
                />
              </label>
              <div className="actions">
                <button disabled={busy}>
                  {editing ? "수정 저장" : "등록"}
                </button>
                <button type="button" className="secondary" onClick={reset}>
                  입력 초기화
                </button>
              </div>
            </form>
          </section>
        </aside>
        <div>
          <section className="card">
            <h2>
              게시글 목록 <small>{posts.length}개</small>
            </h2>
            {posts.length === 0 ? (
              <p>첫 번째 기록을 남겨 보세요.</p>
            ) : (
              posts.map((p) => (
                <article className="post" key={p.id}>
                  <button
                    className="post-title"
                    disabled={busy}
                    onClick={() =>
                      run(
                        async () => setSelected(await api.detail(p.id)),
                        "게시글을 불러왔습니다.",
                      )
                    }
                  >
                    {p.title}
                  </button>
                  <p>
                    <small>
                      {p.authorName} · {new Date(p.createdAt).toLocaleString()}
                    </small>
                  </p>
                </article>
              ))
            )}
          </section>
          <section className="card">
            <h2>게시글 상세</h2>
            {selected ? (
              <>
                <h3>{selected.title}</h3>
                <p className="content">{selected.content}</p>
                <p>
                  <small>작성자 {selected.authorName}</small>
                </p>
                {canEdit(selected) && (
                  <div className="actions">
                    <button
                      className="secondary"
                      onClick={() => {
                        setEditing(selected.id);
                        setTitle(selected.title);
                        setContent(selected.content);
                      }}
                    >
                      수정
                    </button>
                    <button
                      className="danger"
                      disabled={busy}
                      onClick={() => {
                        if (confirm("게시글을 삭제할까요?"))
                          run(async () => {
                            await api.remove(selected.id);
                            setSelected(null);
                            reset();
                            await refresh();
                          }, "게시글을 삭제했습니다.");
                      }}
                    >
                      삭제
                    </button>
                  </div>
                )}
              </>
            ) : (
              <p className="muted">목록에서 제목을 선택하세요.</p>
            )}
          </section>
        </div>
      </div>
    </main>
  );
}
createRoot(document.getElementById("root")).render(<App />);
