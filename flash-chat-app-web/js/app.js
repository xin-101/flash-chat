const App = {
  _conv: [], _curId: '', _mp: 1, _more: true,
  _cd: 0, _ct: null, _sendingCode: false,
  _panel: 'chat', _momentPage: 1, _momentMore: true,
  _mImgs: [], _tmpIds: new Set(),
  _friends: [], _gmems: [], _ginfo: null,
  _pendingFRs: new Set(), _confirmR: null, _modalR: null,

  DFLT: DefaultAvatar,

  $(id) { return document.getElementById(id) },

  init() {
    console.log('[App] init, protocol:', window.location.protocol, 'loggedIn:', Storage.isLoggedIn());
    this._bindEvents();
    if (Storage.isLoggedIn()) {
      const u = Storage.getUserInfo();
      console.log('[App] auto-login user:', u?.id);
      this._showMain(u);
    }
  },

  // ========== Events ==========
  _bindEvents() {
    const $ = this.$;
    $('login-phone').addEventListener('input', () => this._toggleLogin());
    $('login-code').addEventListener('input', () => this._toggleLogin());
    $('login-phone').addEventListener('keydown', e => { if (e.key === 'Enter') $('login-code').focus() });
    $('login-code').addEventListener('keydown', e => { if (e.key === 'Enter') this._doLogin() });
    $('code-btn').addEventListener('click', () => this._sendCode());
    $('login-btn').addEventListener('click', () => this._doLogin());
    $('msg-input').addEventListener('keydown', e => { if (e.key === 'Enter') this._sendMessage() });
    $('msg-input').addEventListener('input', () => this._toggleSend());
    $('send-btn').addEventListener('click', () => this._sendMessage());
    $('search-keyword').addEventListener('input', () => this._doSearch());
    $('msg-list').addEventListener('scroll', () => {
      const el = $('msg-list');
      if (el.scrollTop < 50 && this._more && this._curId) this._loadMsgs(true);
    });
    $('moment-list').addEventListener('scroll', () => {
      const el = $('moment-list');
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100 && this._momentMore) this._loadMoments();
    });
    document.querySelectorAll('.sidebar-tab').forEach(btn => {
      btn.addEventListener('click', () => this._switchPanel(btn.dataset.tab));
    });
    ChatWebSocket.onMessage(d => this._onWs(d));
  },

  toast(m, d = 2000) {
    const e = this.$('toast');
    e.textContent = m;
    e.classList.add('show');
    clearTimeout(e._t);
    e._t = setTimeout(() => e.classList.remove('show'), d);
  },

  // ========== Modal / Confirm ==========
  showModal(title, html) {
    return new Promise(r => {
      this._modalR = r;
      this.$('modal-title').textContent = title;
      this.$('modal-body').innerHTML = html;
      this.$('modal').classList.add('show');
    });
  },
  _closeModal() {
    this.$('modal').classList.remove('show');
    if (this._modalR) { this._modalR(null); this._modalR = null }
  },
  confirm(title, msg) {
    return new Promise(r => {
      this._confirmR = r;
      this.$('confirm-title').textContent = title;
      this.$('confirm-content').textContent = msg;
      this.$('confirm').classList.add('show');
    });
  },
  _closeConfirm(v) {
    this.$('confirm').classList.remove('show');
    if (this._confirmR) { this._confirmR(v); this._confirmR = null }
  },

  // ========== Login ==========
  _toggleLogin() {
    const p = this.$('login-phone').value.trim(),
      c = this.$('login-code').value.trim(),
      pv = /^1\d{10}$/.test(p);
    this.$('code-btn').className = 'code-btn' + (pv && !this._cd && !this._sendingCode ? '' : ' disabled');
    this.$('login-btn').className = 'login-btn' + (pv && c.length === 6 ? '' : ' disabled');
  },
  async _sendCode() {
    const p = this.$('login-phone').value.trim();
    if (!/^1\d{10}$/.test(p) || this._cd > 0 || this._sendingCode) return;
    this._sendingCode = true;
    this._toggleLogin();
    try {
      await AuthAPI.sendCode(p);
      this.toast('验证码已发送');
      this._cd = 60;
      this._updCodeBtn();
      this._ct = setInterval(() => {
        this._cd--;
        if (this._cd <= 0) { clearInterval(this._ct); this._cd = 0 }
        this._updCodeBtn();
        this._toggleLogin();
      }, 1000);
    } catch (e) { this.toast(e.message || '发送失败') } finally { this._sendingCode = false; this._toggleLogin() }
  },
  _updCodeBtn() {
    this.$('code-btn').textContent = this._cd > 0 ? this._cd + 's' : this._sendingCode ? '发送中' : '获取验证码';
  },
  async _doLogin() {
    const p = this.$('login-phone').value.trim(),
      c = this.$('login-code').value.trim();
    if (!/^1\d{10}$/.test(p) || c.length !== 6) return;
    const btn = this.$('login-btn');
    btn.classList.add('disabled');
    btn.textContent = '登录中...';
    try {
      const r = await AuthAPI.login(p, c);
      if (r.data) {
        Storage.setToken(r.data.token);
        Storage.setUserInfo(r.data);
        this._showMain(r.data);
      }
    } catch (e) { this.toast(e.message || '登录失败') } finally { btn.classList.remove('disabled');
      btn.textContent = '登 录' }
  },

  _showMain(u) {
    this.$('page-login').style.display = 'none';
    this.$('page-main').style.display = 'flex';
    this._renderSidebar(u);
    ChatWebSocket.connect(u.token);
    this._loadConvs();
    this._loadFriends();
    this.toast('登录成功');
  },

  // ========== Sidebar ==========
  _renderSidebar(u) {
    this.$('my-name-sidebar').textContent = u.nickname || '用户';
    const a = this.$('my-avatar-sidebar');
    a.src = imgUrl(u.face);
    a.onerror = () => { a.src = DefaultAvatar };
  },
  _switchPanel(p) {
    this._panel = p;
    document.querySelectorAll('.panel').forEach(el => el.style.display = 'none');
    const panel = this.$('panel-' + p);
    if (panel) panel.style.display = '';
    document.querySelectorAll('.sidebar-tab').forEach(b => b.classList.toggle('active', b.dataset.tab === p));
    if (p === 'friend') { this._loadFriends();
      this._loadReqs() }
    if (p === 'moment') { this._momentPage = 1;
      this._momentMore = true;
      this._loadMoments() }
  },

  // ========== Profile ==========
  _showProfile() {
    const u = Storage.getUserInfo();
    if (!u) return;
    this.showModal('编辑资料', `
      <div class="profile-avatar-edit"><img id="edit-avatar" src="${imgUrl(u.face)}" onclick="App._editAvatar()" title="点击更换头像"></div>
      <div class="profile-field"><label>昵称</label><input id="edit-nickname" value="${escHtml(u.nickname || '')}"></div>
      <div class="profile-field"><label>个性签名</label><textarea id="edit-signature">${escHtml(u.signature || '')}</textarea></div>
      <div class="profile-field"><label>闪聊号</label><input id="edit-flash-chat-num" value="${escHtml(u.flashChatNum || '')}" placeholder="30天可修改一次"></div>
      <button class="profile-save" onclick="App._saveProfile()">保存</button>
    `);
  },
  _editAvatar() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = async () => {
      const file = input.files[0];
      if (!file) return;
      try {
        const r = await FileAPI.upload(file, 'avatar');
        if (r.data?.url) {
          this.$('edit-avatar').src = AppConfig.baseUrl + '/' + r.data.url;
          this._newAvatar = r.data.url;
        }
      } catch (e) { this.toast('上传失败') }
    };
    input.click();
  },
  async _saveProfile() {
    const u = Storage.getUserInfo(),
      data = {};
    const nickname = this.$('edit-nickname').value.trim();
    if (nickname && nickname !== u.nickname) data.nickname = nickname;
    const signature = this.$('edit-signature').value.trim();
    if (signature !== (u.signature || '')) data.signature = signature;
    if (this._newAvatar) data.face = this._newAvatar;
    const flashChatNum = this.$('edit-flash-chat-num').value.trim();
    try {
      if (Object.keys(data).length) {
        await AuthAPI.updateUserInfo(data);
        const nu = { ...u, ...data };
        Storage.setUserInfo(nu);
        this._renderSidebar(nu);
      }
      if (flashChatNum && flashChatNum !== u.flashChatNum) {
        await AuthAPI.updateFlashChatNum(flashChatNum);
        const nu = { ...Storage.getUserInfo(), flashChatNum };
        Storage.setUserInfo(nu);
      }
      this.toast('保存成功');
      this._closeModal();
    } catch (e) { this.toast(e.message || '保存失败') }
  },

  // ========== Conversations ==========
  async _loadConvs() {
    try { const r = await ChatAPI.getConversationList();
      this._conv = r.data || [];
      this._renderConvs() } catch (e) {}
  },
  _renderConvs() {
    const el = this.$('conversation-list');
    if (!this._conv.length) { el.innerHTML = '<div class="empty-list">暂无会话</div>'; return }
    el.innerHTML = this._conv.map(c => `
      <div class="conv-item${c.id === this._curId ? ' active' : ''}" onclick="App._openChat('${c.id}','${escHtml(c.name || '用户')}','${escHtml(c.face || '')}',${c.type || 1})">
        <img class="conv-avatar" src="${imgUrl(c.face)}" onerror="this.src=DefaultAvatar" alt="">
        <div class="conv-info">
          <div class="conv-top"><span class="conv-name">${escHtml(c.name || '用户')}${c.type == 2 ? ' <span class="conv-tag">群</span>' : ''}</span><span class="conv-time">${formatBriefTime(c.lastMessageTime)}</span></div>
          <div class="conv-bottom"><span class="conv-msg">${escHtml(c.lastMessage || '暂无消息')}</span>${c.unreadCount > 0 ? '<div class="conv-badge"><span>' + (c.unreadCount > 99 ? '99+' : c.unreadCount) + '</span></div>' : ''}</div>
        </div>
      </div>`).join('');
  },

  // ========== Friends ==========
  async _loadFriends() {
    try { const r = await FriendAPI.getFriendList();
      this._friends = r.data || [];
      this._friends.forEach(f => this._pendingFRs.delete(f.friendId || f.id));
      this._renderFriends() } catch (e) {}
  },
  _renderFriends() {
    const el = this.$('friend-list');
    if (!this._friends.length) { el.innerHTML = '<div class="empty-list">暂无好友，搜索添加吧</div>'; return }
    el.innerHTML = this._friends.map(f => `
      <div class="friend-item" onclick="App._openChatFr('${f.friendId || f.id}','${escHtml(f.remark || f.nickname || '用户')}','${escHtml(f.face || '')}')">
        <img class="f-avatar" src="${imgUrl(f.face)}" onerror="this.src=DefaultAvatar" alt="">
        <div class="f-info">
          <span class="f-name">${escHtml(f.remark || f.nickname || '用户')}</span>
          ${f.remark && f.nickname ? '<span class="f-remark">' + escHtml(f.nickname) + '</span>' : ''}
        </div>
        <span class="f-more" onclick="event.stopPropagation();App._friendMenu('${f.friendId || f.id}','${escHtml(f.remark || f.nickname || '用户')}')">⋮</span>
      </div>`).join('');
  },
  async _loadReqs() {
    try {
      const r = await FriendAPI.getRequests();
      const list = r.data || [];
      const sec = this.$('request-section');
      if (!list.length) { sec.style.display = 'none'; return }
      sec.style.display = '';
      this.$('request-badge').textContent = list.length;
      this.$('request-list').innerHTML = list.map(r => `
        <div class="request-item">
          <img class="r-avatar" src="${imgUrl(r.fromFace)}" onerror="this.src=DefaultAvatar" alt="">
          <div class="r-info"><span class="r-name">${escHtml(r.fromNickname || '用户')}</span>${r.remark ? '<span class="r-remark">' + escHtml(r.remark) + '</span>' : ''}</div>
          <div class="r-actions"><button class="r-btn approve" onclick="App._handleReq('${r.id}','approve',this)">✓</button><button class="r-btn reject" onclick="App._handleReq('${r.id}','reject',this)">✕</button></div>
        </div>`).join('');
    } catch (e) {}
  },
  async _handleReq(id, action, btn) {
    try {
      action === 'approve' ? await FriendAPI.approveRequest(id) : await FriendAPI.rejectRequest(id);
      btn.closest('.request-item').remove();
      this._loadFriends();
      this._loadReqs();
      this.toast(action === 'approve' ? '已添加好友' : '已拒绝');
    } catch (e) { this.toast(e.message || '操作失败') }
  },
  _friendMenu(fid, name) {
    this.showModal(name, `
      <div class="profile-field"><label>备注</label><input id="remark-input" placeholder="设置备注名"></div>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button class="gm-btn primary" style="width:auto;padding:8px 16px" onclick="App._setRemark('${fid}')">设置备注</button>
        <button class="gm-btn danger" style="width:auto;padding:8px 16px" onclick="App._doBlock('${fid}')">拉黑</button>
        <button class="gm-btn danger" style="width:auto;padding:8px 16px" onclick="App._doDelFriend('${fid}')">删除好友</button>
      </div>
    `);
  },
  async _setRemark(fid) { const r = this.$('remark-input').value.trim(); if (!r) return; try { await FriendAPI.setRemark(fid, r);
      this.toast('已设置');
      this._closeModal();
      this._loadFriends() } catch (e) { this.toast(e.message || '设置失败') } },
  async _doBlock(fid) { this._closeModal(); const ok = await this.confirm('拉黑', '确定拉黑该好友？'); if (!ok) return; try { await FriendAPI.block(fid);
      this.toast('已拉黑');
      this._loadFriends() } catch (e) { this.toast(e.message || '操作失败') } },
  async _doDelFriend(fid) { this._closeModal(); const ok = await this.confirm('删除好友', '确定删除该好友？'); if (!ok) return; try { await FriendAPI.deleteFriend(fid);
      this.toast('已删除');
      this._loadFriends() } catch (e) { this.toast(e.message || '操作失败') } },

  // ========== Moments ==========
  async _loadMoments() {
    const el = this.$('moment-list');
    if (this._momentPage === 1) el.innerHTML = '<div style="padding:20px;text-align:center"><div class="spinner"></div></div>';
    try {
      const r = await MomentAPI.list(this._momentPage, 10);
      const list = r.data?.records || r.data || [];
      if (this._momentPage === 1) el.innerHTML = '';
      list.forEach(m => { el.insertAdjacentHTML('beforeend', this._renderMoment(m)) });
      this._momentPage++;
      if (list.length < 10) this._momentMore = false;
    } catch (e) {
      if (this._momentPage === 1) el.innerHTML = '<div class="empty-list">暂无动态</div>';
    }
  },
  _renderMoment(m) {
    const imgs = (m.images || '').split(',').filter(Boolean);
    return `<div class="moment-item">
      <div class="moment-header"><img class="moment-avatar" src="${imgUrl(m.senderFace)}" onerror="this.src=DefaultAvatar"><div><div class="moment-author">${escHtml(m.senderName || '用户')}</div><div class="moment-time">${formatChatTime(m.createTime)}</div></div></div>
      <div class="moment-content">${escHtml(m.content || '')}</div>
      ${imgs.length ? '<div class="mthumb-list">' + imgs.map(img => '<img src="' + (img.startsWith('http') ? img : AppConfig.baseUrl + '/' + img) + '" onclick="event.stopPropagation();window.open(this.src)">').join('') + '</div>' : ''}
      <div class="moment-actions"><span class="${m.liked ? 'liked' : ''}" onclick="App._toggleLike('${m.id}',${!!m.liked})">${m.liked ? '❤️' : '🤍'} ${m.likeCount || 0}</span><span onclick="App._showMomentDetail('${m.id}')">💬 ${m.commentCount || 0}</span></div>
    </div>`;
  },
  _createMoment() { this.$('slide-moment-create').classList.add('show') },
  async _doCreateMoment() {
    const content = this.$('moment-content').value.trim();
    if (!content && !this._mImgs.length) { this.toast('请输入内容或添加图片'); return }
    try {
      await MomentAPI.create(content, this._mImgs);
      this.toast('发布成功');
      this.$('slide-moment-create').classList.remove('show');
      this.$('moment-content').value = '';
      this._mImgs = [];
      this.$('moment-thumbs').innerHTML = '';
      this._momentPage = 1;
      this._momentMore = true;
      this._loadMoments();
    } catch (e) { this.toast(e.message || '发布失败') }
  },
  _chooseMomentImages() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.multiple = true;
    input.onchange = async () => {
      for (const file of input.files) {
        try { const r = await FileAPI.upload(file, 'moment'); if (r.data?.url) this._mImgs.push(r.data.url) } catch (e) {}
      }
      this.$('moment-thumbs').innerHTML = this._mImgs.map(u => '<img src="' + (u.startsWith('http') ? u : AppConfig.baseUrl + '/' + u) + '">').join('');
    };
    input.click();
  },
  async _showMomentDetail(id) {
    try {
      const r = await MomentAPI.list(1, 1);
      const list = r.data?.records || r.data || [];
      const m = list.find(i => i.id == id);
      if (!m) { this.toast('未找到动态'); return }
      const imgs = (m.images || '').split(',').filter(Boolean);
      this.showModal('动态详情', `
        <div style="display:flex;align-items:center;margin-bottom:12px">
          <img src="${imgUrl(m.senderFace)}" style="width:40px;height:40px;border-radius:50%;object-fit:cover;margin-right:10px;background:#e0e0e0">
          <div><div style="font-weight:600">${escHtml(m.senderName || '用户')}</div><div style="font-size:11px;color:#999">${formatChatTime(m.createTime)}</div></div>
        </div>
        <div style="font-size:14px;line-height:1.6;margin-bottom:12px">${escHtml(m.content || '')}</div>
        ${imgs.length ? '<div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:12px">' + imgs.map(img => '<img src="' + (img.startsWith('http') ? img : AppConfig.baseUrl + '/' + img) + '" style="max-width:200px;max-height:200px;object-fit:cover;border-radius:6px;cursor:pointer" onclick="window.open(this.src)">').join('') + '</div>' : ''}
        <div style="display:flex;gap:16px;padding:12px 0;border-top:1px solid var(--border);margin-bottom:12px">
          <span style="cursor:pointer;font-size:13px;color:${m.liked ? '#f44336' : '#666'}" onclick="App._toggleLike('${m.id}',${!!m.liked});App._closeModal()">${m.liked ? '❤️' : '🤍'} ${m.likeCount || 0}</span>
          <span style="font-size:13px;color:#666">💬 ${m.commentCount || 0}</span>
        </div>
        ${(m.comments || []).map(c => '<div style="font-size:13px;padding:4px 0;color:#666"><span style="color:#3b82f6;font-weight:500">' + escHtml(c.nickname || '用户') + '</span>' + (c.replyUserId ? ' <span style="color:#999">回复</span> <span style="color:#3b82f6;font-weight:500">' + escHtml(c.replyNickname || '') + '</span>' : '') + '：' + escHtml(c.content || '') + '</div>').join('')}
      `);
    } catch (e) { this.toast('加载失败') }
  },
  async _toggleLike(id, liked) {
    try { liked ? await MomentAPI.unlike(id) : await MomentAPI.like(id);
      this._momentPage = 1;
      this._momentMore = true;
      this._loadMoments() } catch (e) {}
  },

  // ========== Search ==========
  _showSearch() { this.$('search-area').style.display = 'flex';
    this.$('search-keyword').focus() },
  _hideSearch() {
    this.$('search-area').style.display = 'none';
    this.$('search-keyword').value = '';
    this.$('search-results').innerHTML = '';
  },
  async _doSearch() {
    const kw = this.$('search-keyword').value.trim(),
      el = this.$('search-results');
    if (!kw) { el.innerHTML = ''; return }
    el.innerHTML = '<div style="padding:16px;text-align:center;color:#888;font-size:12px">搜索中...</div>';
    try {
      const r = await ChatAPI.searchUser(kw);
      const list = (r.data || []).filter(u => u.id != Storage.getUserId());
      if (!list.length) { el.innerHTML = '<div style="padding:24px;text-align:center;color:#888;font-size:12px">未找到用户</div>'; return }
      el.innerHTML = list.map(u => {
        const isFriend = this._friends.some(f => f.friendId === u.id || f.id === u.id);
        const isPending = this._pendingFRs.has(u.id);
        return `<div class="s-user-item">
          <img class="su-avatar" src="${imgUrl(u.face)}" onerror="this.src=DefaultAvatar">
          <div class="su-info"><span class="su-name">${escHtml(u.nickname || '用户')}</span>${u.mobile ? '<span class="su-mobile">' + escHtml(u.mobile) + '</span>' : ''}</div>
          <div class="su-actions">
            ${isFriend
              ? '<button class="su-btn chat" onclick="App._startChat(\'' + u.id + '\',\'' + escHtml(u.nickname || '用户') + '\',\'' + escHtml(u.face || '') + '\')">聊天</button>'
              : isPending
                ? '<button class="su-btn sent" disabled>已申请</button>'
                : '<button class="su-btn friend" onclick="App._sendFR(\'' + u.id + '\',\'' + escHtml(u.nickname || '用户') + '\',this)">加好友</button>'}
          </div>
        </div>`;
      }).join('');
    } catch (e) { el.innerHTML = `<div style="padding:24px;text-align:center;color:#888;font-size:12px">${escHtml(e.message || '搜索失败')}</div>` }
  },
  async _startChat(uid, name, avatar) {
    if (!uid || uid == Storage.getUserId()) { this.toast('不能和自己聊天'); return }
    const isFriend = this._friends.some(f => f.friendId === uid || f.id === uid);
    if (!isFriend) { this.toast('请先添加好友'); return }
    this.toast('创建会话...', 1000);
    try {
      const r = await ChatAPI.createConversation(uid);
      if (r.data) {
        this._hideSearch();
        this._openChat(r.data.id || r.data, name, avatar, 1);
      }
    } catch (e) { this.toast(e.message || '创建会话失败') }
  },
  async _sendFR(uid, nickname, btn) {
    if (!uid || uid == Storage.getUserId()) return;
    btn.classList.add('sent');
    btn.textContent = '已申请';
    btn.onclick = null;
    try {
      await FriendAPI.sendRequest(uid, '你好，我是' + (Storage.getUserInfo()?.nickname || '用户'));
      this._pendingFRs.add(uid);
      this.toast('好友申请已发送');
    } catch (e) {
      btn.classList.remove('sent');
      btn.textContent = '加好友';
      btn.onclick = function() { App._sendFR(uid, nickname, this) };
      this.toast(e.message || '发送失败');
    }
  },

  // ========== Chat ==========
  _openChatFr(fid, name, avatar) {
    this._switchPanel('chat');
    this._startChat(fid, name, avatar);
  },
  _openChat(convId, name, avatar, type) {
    this._curId = convId;
    this._mp = 1;
    this._more = true;
    this.$('empty-state').style.display = 'none';
    this.$('chat-area').style.display = 'flex';
    this.$('detail-name').textContent = name;
    const ae = this.$('detail-avatar');
    ae.src = imgUrl(avatar);
    ae.onerror = () => { ae.src = DefaultAvatar };
    this.$('detail-type').textContent = type == 2 ? '群聊' : '单聊';
    this.$('group-manage-btn').style.display = type == 2 ? '' : 'none';
    this.$('msg-list').innerHTML = '<div style="padding:20px;text-align:center"><div class="spinner"></div></div>';
    this._renderConvs();
    this._loadMsgs(false);
    this.$('msg-input').focus();
    const c = this._conv.find(i => i.id === convId);
    if (c) { c.unreadCount = 0;
      this._renderConvs() }
    ChatAPI.markAsRead(convId).catch(() => {});
    if (type == 2) this._ginfo = { id: convId, name };
  },
  async _loadMsgs(more) {
    if (more) { if (!this._more) return;
      this._mp++ } else { this._mp = 1;
      this._more = true }
    try {
      const r = await ChatAPI.getMessageList(this._curId, this._mp, 20);
      const list = (r.data || []).reverse();
      this._renderMsgs(list, more);
      if (list.length < 20) this._more = false;
    } catch (e) {
      if (!more) this.$('msg-list').innerHTML = '<div class="empty-list" style="padding:60px 0">暂无消息，发送第一条开始聊天</div>';
    }
  },
  _renderMsgs(list, append) {
    const el = this.$('msg-list'),
      myId = Storage.getUserId();
    if (!append) {
      if (!list.length) { el.innerHTML = '<div style="padding:60px 0;text-align:center;color:#999;font-size:13px">暂无消息，发送第一条开始聊天</div>'; return }
      el.innerHTML = '';
    }
    const html = list.map(m => {
      const isMe = m.senderId == myId;
      const isImg = m.msgType == 2 || m.type == 2;
      const as = isMe ? imgUrl(Storage.getUserInfo()?.face) : this.$('detail-avatar').src;
      return `<div class="msg-row ${isMe ? 'me' : ''}">
        <img class="msg-avatar" src="${as}" onerror="this.src=DefaultAvatar">
        <div class="msg-body ${isMe ? 'me' : ''}">
          <div class="msg-bubble ${isMe ? 'me' : ''}">${isImg ? '<img class="msg-image" src="' + (m.content.startsWith('http') ? m.content : AppConfig.baseUrl + '/' + m.content) + '" onclick="window.open(this.src)">' : '<span class="msg-text">' + escHtml(m.content || '') + '</span>'}</div>
          <span class="msg-time">${formatMessageTime(m.createTime)}</span>
        </div>
      </div>`;
    }).join('');
    append ? el.insertAdjacentHTML('afterbegin', html) : (el.innerHTML = html, this._scrollBottom());
  },
  _toggleSend() {
    const i = this.$('msg-input'),
      b = this.$('send-btn');
    i.value.trim() ? b.classList.remove('disabled') : b.classList.add('disabled');
  },
  async _sendMessage() {
    const input = this.$('msg-input'),
      content = input.value.trim();
    if (!content || !this._curId) return;
    const btn = this.$('send-btn');
    btn.classList.add('disabled');
    input.value = '';
    const tid = 'tmp_' + Date.now();
    this._tmpIds.add(tid);
    const el = this.$('msg-list');
    const emp = el.querySelector('.msg-empty');
    if (emp) emp.remove();
    const face = imgUrl(Storage.getUserInfo()?.face);
    el.insertAdjacentHTML('beforeend', `<div class="msg-row me" id="${tid}"><img class="msg-avatar" src="${face}"><div class="msg-body me"><div class="msg-bubble me"><span class="msg-text">${escHtml(content)}</span></div><span class="msg-time">${formatMessageTime(new Date().toISOString())}</span></div></div>`);
    this._scrollBottom();
    try {
      await ChatAPI.sendTextMessage(this._curId, content);
      const t = document.getElementById(tid);
      if (t) { t.removeAttribute('id');
        t.querySelector('.msg-time').textContent = formatMessageTime(new Date().toISOString()) }
      this._scrollBottom();
    } catch (e) {
      const t = document.getElementById(tid);
      if (t) t.remove();
      this.toast(e.message || '发送失败');
    } finally { this._tmpIds.delete(tid) }
  },
  _scrollBottom() { const e = this.$('msg-list');
    requestAnimationFrame(() => { e.scrollTop = e.scrollHeight }) },
  _chooseImage() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = async () => {
      const file = input.files[0];
      if (!file) return;
      if (!this._curId) { this.toast('请先选择会话'); return }
      try {
        const r = await FileAPI.upload(file, 'chat');
        if (r.data?.url) { await ChatAPI.sendImageMessage(this._curId, r.data.url);
          this.toast('图片已发送') }
      } catch (e) { this.toast('发送失败') }
    };
    input.click();
  },

  // ========== Group ==========
  async _showGroupManage() {
    if (!this._ginfo) return;
    const el = this.$('slide-group');
    el.classList.add('show');
    const body = this.$('group-manage-body');
    body.innerHTML = '<div style="padding:20px;text-align:center"><div class="spinner"></div></div>';
    try {
      const [info, members] = await Promise.all([
        GroupAPI.getInfo(this._curId),
        GroupAPI.getMembersDetail(this._curId),
      ]);
      const memList = members.data || [];
      const gInfo = info.data || {};
      const myId = Storage.getUserId();
      const isOwner = gInfo.ownerId == myId;
      body.innerHTML = `
        <div class="gm-header"><img class="gm-avatar" src="${this.DFLT}"><div class="gm-info"><div class="gm-name">${escHtml(this._ginfo.name)}</div><div class="gm-count">${memList.length} 人</div></div></div>
        <div class="gm-section">
          <div class="gm-section-title">群名称</div>
          <input class="gm-input" id="gm-name-input" value="${escHtml(this._ginfo.name)}" ${isOwner ? '' : 'disabled'}>
          ${isOwner ? '<button class="gm-btn primary" onclick="App._updGroupName()">修改名称</button>' : ''}
        </div>
        <div class="gm-section">
          <div class="gm-section-title">群成员</div>
          ${memList.map(m => {
            const role = m.userId == gInfo.ownerId ? '群主' : '成员';
            return `<div class="gm-member">
              <img class="gm-member-avatar" src="${imgUrl(m.face)}" onerror="this.src=DefaultAvatar">
              <div class="gm-member-info"><span class="gm-member-name">${escHtml(m.nickname || '用户')}</span><span class="gm-member-role">${role}</span></div>
              ${isOwner && m.userId != myId ? '<div class="gm-member-actions"><button class="gm-member-action transfer" onclick="App._transferOwner(\'' + m.userId + '\')">转让</button><button class="gm-member-action kick" onclick="App._kickMember(\'' + m.userId + '\')">踢出</button></div>' : ''}
            </div>`;
          }).join('')}
        </div>
        <div class="gm-section">
          <div class="gm-section-title">邀请成员</div>
          <button class="gm-btn primary" onclick="App._inviteMembers()">从好友中选择</button>
        </div>
        ${isOwner ? '<div class="gm-section"><button class="gm-btn danger" onclick="App._quitGroup()">解散群聊</button></div>' : '<div class="gm-section"><button class="gm-btn danger" onclick="App._quitGroup()">退出群聊</button></div>'}
      `;
    } catch (e) { body.innerHTML = '<div style="padding:20px;text-align:center;color:#999">加载失败</div>' }
  },
  _hideGroupManage() { this.$('slide-group').classList.remove('show') },
  async _updGroupName() {
    const name = this.$('gm-name-input').value.trim();
    if (!name) return;
    try {
      await GroupAPI.updateName(this._curId, name);
      this.toast('已修改');
      this._ginfo.name = name;
      this.$('detail-name').textContent = name;
      this._hideGroupManage();
      this._loadConvs();
    } catch (e) { this.toast(e.message || '修改失败') }
  },
  async _transferOwner(uid) {
    this._hideGroupManage();
    const ok = await this.confirm('转让群主', '确定转让群主？');
    if (!ok) return;
    try {
      await GroupAPI.transferOwner(this._curId, uid);
      this.toast('已转让');
      this._loadConvs();
    } catch (e) { this.toast(e.message || '转让失败') }
  },
  async _kickMember(uid) {
    this._hideGroupManage();
    const ok = await this.confirm('踢出群聊', '确定踢出该成员？');
    if (!ok) return;
    try {
      await GroupAPI.removeMember(this._curId, uid);
      this.toast('已踢出');
      this._showGroupManage();
    } catch (e) { this.toast(e.message || '操作失败') }
  },
  async _quitGroup() {
    this._hideGroupManage();
    const ok = await this.confirm('退出群聊', '确定退出该群聊？');
    if (!ok) return;
    try {
      await GroupAPI.removeMember(this._curId, Storage.getUserId());
      this.toast('已退出');
      this.$('chat-area').style.display = 'none';
      this.$('empty-state').style.display = 'flex';
      this._curId = '';
      this._loadConvs();
    } catch (e) { this.toast(e.message || '操作失败') }
  },
  async _inviteMembers() {
    const friends = this._friends.filter(f => !(this._gmems || []).some(m => m.userId == (f.friendId || f.id)));
    if (!friends.length) { this.toast('没有可邀请的好友'); return }
    const html = friends.map(f =>
      `<label class="invite-item"><input type="checkbox" class="invite-cb" value="${f.friendId || f.id}"><img src="${imgUrl(f.face)}"><span>${escHtml(f.remark || f.nickname || '用户')}</span></label>`
    ).join('');
    this.showModal('邀请成员', `${html}<button class="profile-save" style="margin-top:12px" onclick="App._doInvite()">邀请</button>`);
  },
  async _doInvite() {
    const ids = [...document.querySelectorAll('.invite-cb:checked')].map(cb => cb.value);
    if (!ids.length) { this.toast('请选择好友'); return }
    try {
      await GroupAPI.invite(this._curId, ids);
      this.toast('邀请成功');
      this._closeModal();
      this._showGroupManage();
    } catch (e) { this.toast(e.message || '邀请失败') }
  },

  // ========== WebSocket ==========
  _onWs(data) {
    if (data.type === 'message') {
      if (this._curId && data.conversationId == this._curId) {
        if (document.querySelector(`[data-msg-id="${data.id}"]`)) return;
        const myId = Storage.getUserId(),
          isMe = data.senderId == myId;
        const el = this.$('msg-list');
        if (isMe && data.content) {
          const existing = el.querySelector('.me .msg-text');
          if (existing && existing.textContent === data.content) {
            const row = existing.closest('.msg-row');
            row.querySelector('.msg-time').textContent = formatMessageTime(data.createTime);
            row.setAttribute('data-msg-id', data.id);
            return;
          }
        }
        const face = isMe ? imgUrl(Storage.getUserInfo()?.face) : this.$('detail-avatar').src;
        const isImg = data.msgType == 2 || data.type == 2;
        el.insertAdjacentHTML('beforeend', `<div class="msg-row ${isMe ? 'me' : ''}" data-msg-id="${data.id}"><img class="msg-avatar" src="${face}"><div class="msg-body ${isMe ? 'me' : ''}"><div class="msg-bubble ${isMe ? 'me' : ''}">${isImg ? '<img class="msg-image" src="' + (data.content?.startsWith('http') ? data.content : AppConfig.baseUrl + '/' + data.content) + '" onclick="window.open(this.src)">' : '<span class="msg-text">' + escHtml(data.content || '') + '</span>'}</div><span class="msg-time">${formatMessageTime(data.createTime)}</span></div></div>`);
        this._scrollBottom();
      }
      this._loadConvs();
    } else if (data.type === 'friend_request') {
      if (data.toUserId == Storage.getUserId()) {
        this.toast('收到好友申请：' + (data.fromNickname || '用户'), 4000);
        this._loadReqs();
      }
    }
  },

  // ========== Logout ==========
  async _logout() {
    const ok = await this.confirm('退出登录', '确定要退出登录吗？');
    if (!ok) return;
    AuthAPI.logout().catch(() => {});
    ChatWebSocket.close();
    Storage.clearAuth();
    this._conv = [];
    this._curId = '';
    this._friends = [];
    this.$('page-main').style.display = 'none';
    this.$('chat-area').style.display = 'none';
    this.$('empty-state').style.display = 'flex';
    this.$('page-login').style.display = 'flex';
  },
};
document.addEventListener('DOMContentLoaded', () => App.init());
