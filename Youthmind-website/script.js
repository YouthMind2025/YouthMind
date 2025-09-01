// 等待DOM加载完成
document.addEventListener('DOMContentLoaded', function() {
    
    // 导航栏平滑滚动功能
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // 移除所有活动状态
            navLinks.forEach(nav => nav.classList.remove('active'));
            // 添加当前链接的活动状态
            this.classList.add('active');
            
            // 获取目标元素
            const targetId = this.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            
            if (targetElement) {
                // 计算滚动位置（考虑固定导航栏高度）
                const navHeight = document.querySelector('.navbar').offsetHeight;
                const targetPosition = targetElement.offsetTop - navHeight;
                
                // 平滑滚动
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // 滚动时更新导航栏活动状态
    window.addEventListener('scroll', function() {
        const sections = document.querySelectorAll('section');
        const navHeight = document.querySelector('.navbar').offsetHeight;
        const scrollPosition = window.scrollY + navHeight + 100;
        
        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.offsetHeight;
            const sectionId = section.getAttribute('id');
            
            if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
                // 移除所有活动状态
                navLinks.forEach(nav => nav.classList.remove('active'));
                // 添加对应链接的活动状态
                const activeLink = document.querySelector(`.nav-link[href="#${sectionId}"]`);
                if (activeLink) {
                    activeLink.classList.add('active');
                }
            }
        });
        
        // 导航栏滚动效果
        const navbar = document.querySelector('.navbar');
        if (window.scrollY > 50) {
            navbar.style.background = 'rgba(255, 255, 255, 0.95)';
            navbar.style.backdropFilter = 'blur(10px)';
        } else {
            navbar.style.background = 'var(--white)';
            navbar.style.backdropFilter = 'none';
        }
    });
    
    // 联系表单处理
    const contactForm = document.querySelector('.contact-form');
    if (contactForm) {
        contactForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // 获取表单数据
            const name = this.querySelector('input[type="text"]').value;
            const email = this.querySelector('input[type="email"]').value;
            const message = this.querySelector('textarea').value;
            
            // 简单验证
            if (!name || !email || !message) {
                showMessage('请填写所有必填字段', 'error');
                return;
            }
            
            // 邮箱格式验证
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                showMessage('请输入有效的邮箱地址', 'error');
                return;
            }
            
            // 模拟表单提交
            const submitBtn = this.querySelector('.btn-submit');
            const originalText = submitBtn.textContent;
            submitBtn.textContent = '发送中...';
            submitBtn.disabled = true;
            
            // 模拟异步提交
            setTimeout(() => {
                showMessage('消息发送成功！我们会尽快回复您。', 'success');
                this.reset();
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }, 1500);
        });
    }
    
    // 显示消息提示
    function showMessage(text, type) {
        // 创建消息元素
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = text;
        
        // 添加样式
        messageDiv.style.cssText = `
            position: fixed;
            top: 90px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 8px;
            color: white;
            font-weight: 500;
            z-index: 10000;
            transform: translateX(400px);
            transition: transform 0.3s ease;
            ${type === 'success' 
                ? 'background: linear-gradient(135deg, #10b981, #059669);' 
                : 'background: linear-gradient(135deg, #ef4444, #dc2626);'
            }
        `;
        
        document.body.appendChild(messageDiv);
        
        // 显示动画
        setTimeout(() => {
            messageDiv.style.transform = 'translateX(0)';
        }, 100);
        
        // 自动隐藏
        setTimeout(() => {
            messageDiv.style.transform = 'translateX(400px)';
            setTimeout(() => {
                document.body.removeChild(messageDiv);
            }, 300);
        }, 3000);
    }
    
    // 按钮点击效果
    const buttons = document.querySelectorAll('button');
    buttons.forEach(button => {
        button.addEventListener('click', function(e) {
            // 创建涟漪效果
            const ripple = document.createElement('span');
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;
            
            ripple.style.cssText = `
                position: absolute;
                width: ${size}px;
                height: ${size}px;
                left: ${x}px;
                top: ${y}px;
                background: rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                transform: scale(0);
                animation: ripple 0.6s linear;
                pointer-events: none;
            `;
            
            this.style.position = 'relative';
            this.style.overflow = 'hidden';
            this.appendChild(ripple);
            
            setTimeout(() => {
                if (ripple.parentNode) {
                    ripple.parentNode.removeChild(ripple);
                }
            }, 600);
        });
    });
    
    // 添加涟漪动画CSS
    const style = document.createElement('style');
    style.textContent = `
        @keyframes ripple {
            to {
                transform: scale(4);
                opacity: 0;
            }
        }
        
        /* 文章卡片悬停效果增强 */
        .article-card {
            position: relative;
            overflow: hidden;
        }
        
        .article-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.05), transparent);
            transition: left 0.5s ease;
        }
        
        .article-card:hover::before {
            left: 100%;
        }
        
        /* 统计数字动画 */
        .stat h3 {
            opacity: 0;
            transform: translateY(20px);
            animation: countUp 1s ease forwards;
        }
        
        @keyframes countUp {
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
    `;
    document.head.appendChild(style);
    
    // Intersection Observer for animations
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                
                // 如果是统计数字，启动计数动画
                if (entry.target.classList.contains('stat')) {
                    animateCounter(entry.target.querySelector('h3'));
                }
            }
        });
    }, observerOptions);
    
    // 观察需要动画的元素
    const animatedElements = document.querySelectorAll('.article-card, .stat, .about-text, .contact-info, .contact-form');
    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(el);
    });
    
    // 数字计数动画
    function animateCounter(element) {
        const target = parseInt(element.textContent.replace(/[^\d]/g, ''));
        const suffix = element.textContent.replace(/[\d]/g, '');
        let current = 0;
        const increment = target / 50;
        const timer = setInterval(() => {
            current += increment;
            if (current >= target) {
                element.textContent = target + suffix;
                clearInterval(timer);
            } else {
                element.textContent = Math.floor(current) + suffix;
            }
        }, 30);
    }
    
    // Logo点击回到顶部
    const logo = document.querySelector('.logo a');
    if (logo) {
        logo.addEventListener('click', function(e) {
            if (this.getAttribute('href') === '#home') {
                e.preventDefault();
                window.scrollTo({
                    top: 0,
                    behavior: 'smooth'
                });
                
                // 更新导航状态
                navLinks.forEach(nav => nav.classList.remove('active'));
                document.querySelector('.nav-link[href="#home"]').classList.add('active');
            }
        });
    }
    
    
    // 登录注册弹窗功能
    initAuthModals();
    
    // 初始化3D地球交互
    /* Spinning globe removed */
    
    // 初始化文章功能
    // Load articles from backend API, with fallback to static files
    loadArticlesFromBackend()
        .then(articlesArr => {
            window.__articlesManifest = articlesArr;
            initArticleFeatures(articlesArr);
        })
        .catch(err => {
            console.error('Failed to load articles from backend, trying static fallback', err);
            // Fallback to static articles.json
            fetch('assets/Articles/articles.json')
                .then(r=>r.json())
                .then(articlesArr=>{
                    window.__articlesManifest = articlesArr;
                    initArticleFeatures(articlesArr);
                })
                .catch(staticErr=>{
                    console.error('Failed to load articles from both sources', staticErr);
                });
        });
    
    // 初始化帖子滑动功能
    initPostSlider();
    
    // 初始化静态世界地图
    initWorldMap();
    
});

// Load articles from backend API
async function loadArticlesFromBackend() {
    try {
        // Try to detect if we're running in development (localhost) or production
        const isDevelopment = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
        const baseUrl = isDevelopment ? 'http://localhost:8080' : ''; // Use relative path in production
        
        const response = await fetch(`${baseUrl}/v1/articles`);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        // Backend returns { success: true, data: [...], message: "..." }
        const articles = data.data || data;
        
        // Convert backend article format to website format
        return articles.map((article, index) => ({
            id: article.id || String(index + 1),
            title: article.title,
            date: article.date || new Date(article.createdAt).getFullYear().toString(),
            file: article.file || `${article.title.replace(/[^a-zA-Z0-9\s]/g, '').replace(/\s+/g, ' ').trim()}.html`,
            cover: article.cover || article.coverImage || `cover${index + 1}.jpg`,
            intro: article.intro || article.getSummary?.() || '',
            // Store the full content for direct use
            content: article.content,
            // Flag to indicate this is from backend
            isFromBackend: true
        }));
    } catch (error) {
        console.error('Error loading articles from backend:', error);
        throw error;
    }
}

// 文章功能初始化
function initArticleFeatures(articlesArr) {
    const menuContainer=document.querySelector('.articles-menu');
    const displayContainer=document.querySelector('.article-display');

    // clear containers
    menuContainer.innerHTML='';
    displayContainer.innerHTML='';

    articlesArr.forEach((art,index)=>{
        // menu button
        const btn=document.createElement('button');
        btn.className=`article-menu-btn${index===0?' active':''}`;
        btn.dataset.article=art.id;
        btn.textContent=art.title;
        menuContainer.appendChild(btn);

        // preview card
        const item=document.createElement('div');
        item.className=`article-item${index===0?' active':''}`;
        item.dataset.article=art.id;
        item.innerHTML=`<div class="article-cover"><div class="article-date">${art.date}</div><div class="article-image-placeholder">Loading...</div></div><div class="article-content"><h3 class="article-title">${art.title}</h3><p class="article-intro">${art.intro||''}</p><button class="btn-read-article" data-article="${art.id}">Read this article</button></div>`;
        displayContainer.appendChild(item);
    });

    // 预加载文章图片以填充封面
    preloadArticleImages(articlesArr);

    function preloadArticleImages(arr){
        arr.forEach(a=>{
            // If cover already exists skip
            const itemEl=document.querySelector(`.article-item[data-article="${a.id}"] .article-cover`);
            if(!itemEl) {
                return;
            }
            // If an img already present, skip
            if(itemEl.querySelector('img')) {
                return;
            }

            // For backend articles, extract first image from content
            if(a.isFromBackend && a.content) {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = a.content;
                const firstImg = tempDiv.querySelector('img');
                
                if(firstImg) {
                    const imgSrc = firstImg.getAttribute('src');
                    // Preserve the date element
                    const dateEl = itemEl.querySelector('.article-date');
                    let img = itemEl.querySelector('img');
                    if(!img){
                        img = document.createElement('img');
                        img.style.width='100%';
                        img.style.height='100%';
                        img.style.objectFit='contain';
                        img.style.objectPosition='center';
                        img.style.borderRadius='15px';
                        itemEl.innerHTML='';
                        itemEl.appendChild(img);
                        if(dateEl) itemEl.appendChild(dateEl);
                    }
                    img.src = imgSrc;
                    img.alt = a.title;
                } else if(a.cover || a.coverImage) {
                    // Fallback to cover image if no image in content
                    const dateEl = itemEl.querySelector('.article-date');
                    let img = itemEl.querySelector('img');
                    if(!img){
                        img = document.createElement('img');
                        img.style.width='100%';
                        img.style.height='100%';
                        img.style.objectFit='contain';
                        img.style.objectPosition='center';
                        img.style.borderRadius='15px';
                        itemEl.innerHTML='';
                        itemEl.appendChild(img);
                        if(dateEl) itemEl.appendChild(dateEl);
                    }
                    const src = /^(data:|https?:)/.test(a.cover || a.coverImage) ? (a.cover || a.coverImage) : `assets/Articles/${a.cover || a.coverImage}`;
                    img.src = src;
                    img.alt = a.title;
                } else {
                    // No image found, show placeholder
                    itemEl.innerHTML = `<div class="article-date">${a.date}</div><div class="article-image-placeholder">No Image</div>`;
                }
            } else {
                // For static articles, use existing logic
                loadArticleData(a.id)
                  .then(data=>{
                      if(data.image){
                          let img=itemEl.querySelector('img');
                          if(!img){
                              img=document.createElement('img');
                              img.style.width='100%';
                              img.style.height='100%';
                              img.style.objectFit='contain';
                              img.style.objectPosition='center';
                              img.style.borderRadius='15px';
                              itemEl.innerHTML='';
                              itemEl.appendChild(img);
                          }
                          img.src=/^(data:|https?:)/.test(data.image)?data.image:`assets/Articles/${data.image}`;
                          img.alt=data.title;
                      }
                  })
                  .catch(()=>{});
            }
        });
    }

    const menuButtons=menuContainer.querySelectorAll('.article-menu-btn');
    const articleItems=displayContainer.querySelectorAll('.article-item');
    const readArticleButtons=document.querySelectorAll('.btn-read-article, .btn-team-article');
    
    // 缓存已加载的文章数据
    const articleCache = {};

    // (Removed unused realArticles sample configuration block)

    function loadArticleData(articleId){
        if(articleCache[articleId]) return Promise.resolve(articleCache[articleId]);
        const info = (window.__articlesManifest||[]).find(a=>a.id===articleId);
        if(!info) return Promise.reject('Article not configured');

        // If article is from backend, use the stored content directly
        if(info.isFromBackend && info.content) {
            // Try to pull first image src from HTML content
            let imgSrc='';
            const tmp=document.createElement('div');
            tmp.innerHTML=info.content||'';
            const imgEl=tmp.querySelector('img');
            if(imgEl){
                imgSrc=imgEl.getAttribute('src')||'';
                imgEl.remove(); // remove to avoid duplicate image in popup
            }
            if(!imgSrc && info.cover){
                imgSrc=info.cover; // fallback
            }

            const data={
                title:info.title,
                date:info.date,
                image:imgSrc,
                content:tmp.innerHTML // content without first image
            };
            articleCache[articleId]=data;

            // 更新预览封面图
            if(data.image){
                updateArticleCoverImage(articleId, data);
            }
            return Promise.resolve(data);
        }

        // Fallback to original Word document loading for static articles
        const filePath = `assets/Articles/${info.file}`;
        return fetch(filePath)
            .then(r=>r.arrayBuffer())
            .then(buffer=>mammoth.convertToHtml({arrayBuffer:buffer}))
            .then(result=>{
                // Try to pull first image src (base64) from HTML
                let imgSrc='';
                const tmp=document.createElement('div');
                tmp.innerHTML=result.value||'';
                const imgEl=tmp.querySelector('img');
                if(imgEl){
                    imgSrc=imgEl.getAttribute('src')||'';
                    imgEl.remove(); // remove to avoid duplicate image in popup
                }
                if(!imgSrc && info.cover){
                    imgSrc=info.cover; // fallback
                }

                const data={
                    title:info.title,
                    date:info.date,
                    image:imgSrc,
                    content:tmp.innerHTML // content without first image
                };
                articleCache[articleId]=data;

                // 更新预览封面图
                if(data.image){
                    updateArticleCoverImage(articleId, data);
                }
                return data;
            });
    }

    // Helper function to update article cover image
    function updateArticleCoverImage(articleId, data) {
        const coverHolder=document.querySelector(`.article-item[data-article="${articleId}"] .article-cover`);
        if(coverHolder){
                         let imgEl=coverHolder.querySelector('img');
             if(!imgEl){
                 imgEl=document.createElement('img');
                 imgEl.style.width='100%';
                 imgEl.style.height='100%';
                 imgEl.style.objectFit='contain';
                 imgEl.style.objectPosition='center';
                 imgEl.style.borderRadius='15px';
                 coverHolder.innerHTML='';
                 coverHolder.appendChild(imgEl);
             }
            const src=/^(data:|https?:)/.test(data.image)?data.image:`assets/Articles/${data.image}`;
            imgEl.src=src;
            imgEl.alt=data.title;
        }
    }
    
    // 文章菜单切换功能
    menuButtons.forEach(button => {
        button.addEventListener('click', function() {
            const articleId = this.getAttribute('data-article');
            
            // 更新按钮状态
            menuButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');
            
            // 切换文章显示
            articleItems.forEach(item => {
                item.classList.remove('active');
                if (item.getAttribute('data-article') === articleId) {
                    setTimeout(() => {
                        item.classList.add('active');
                    }, 100);
                }
            });
        });
    });
    
    // 文章详情新标签页功能
    readArticleButtons.forEach(button => {
        button.addEventListener('click', function() {
            const articleId = this.getAttribute('data-article');
            
            // Open article in new tab instead of modal
            const articleUrl = `articles/article-template.html?id=${articleId}`;
            window.open(articleUrl, '_blank');
        });
    });
}

// 帖子滑动功能
function initPostSlider() {
    const slider = document.querySelector('.posts-slider');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    if (!slider || !prevBtn || !nextBtn) return;

    const cardWidth = 325; // 卡片宽度 + 间距

    // 从 JSON 清单加载帖子数据
    fetch('assets/Posts/posts.json')
        .then(r => r.json())
        .then(postsArr => {
            slider.innerHTML = '';

            const postCache = {};

            // helper to load a post's json
            function loadPostData(post){
                if(postCache[post.id]) return Promise.resolve(postCache[post.id]);
                return fetch(`assets/Posts/${post.file}`)
                    .then(r=>r.json())
                    .then(data=>{
                        postCache[post.id]=data;
                        return data;
                    });
            }

            // generate cards after loading all post jsons in parallel
            Promise.all(postsArr.map(p=>loadPostData(p))).then(fullPosts=>{
                fullPosts.forEach(postData=>{
                    // derive summary: first 25 words plain text
                    const tmp=document.createElement('div');
                    tmp.innerHTML=postData.content||'';
                    const text=tmp.textContent||'';
                    const words=text.split(/\s+/).slice(0,25).join(' ')+ '...';

                    const card=document.createElement('div');
                    card.className='post-card';
                    card.innerHTML=`<div class="post-cover"><div class="post-date">${postData.date}</div><div class="post-image">${postData.image}</div></div><div class="post-info"><h3 class="post-title">${postData.title}</h3><p class="post-summary">${words}</p><button class="btn-read-more-post" data-post="${postData.id}">Read More</button></div>`;
                    slider.appendChild(card);
                });

                // bind buttons
                slider.querySelectorAll('.btn-read-more-post').forEach(btn=>{
                    btn.addEventListener('click',function(){
                        const id=this.getAttribute('data-post');
                        const data=postCache[id];
                        if(!data) return;
                        document.getElementById('articleModalTitle').textContent=data.title;
                        document.getElementById('articleModalDate').textContent=data.date;
                        const imageHolder=document.getElementById('articleModalImage');
                        if(imageHolder){imageHolder.textContent=data.image;}
                        document.getElementById('articleModalContent').innerHTML=data.content;
                        showModal('articleModal');
                    });
                });
            });
        })
        .catch(err => console.error('Failed to load posts manifest', err));

    // 左滑动按钮
    prevBtn.addEventListener('click', function () {
        slider.scrollBy({
            left: -cardWidth,
            behavior: 'smooth'
        });
    });

    // 右滑动按钮
    nextBtn.addEventListener('click', function () {
        slider.scrollBy({
            left: cardWidth,
            behavior: 'smooth'
        });
    });

    // 触摸滑动支持
    let startX = 0;
    let scrollLeft = 0;
    let isDown = false;

    slider.addEventListener('mousedown', function (e) {
        isDown = true;
        slider.style.cursor = 'grabbing';
        startX = e.pageX - slider.offsetLeft;
        scrollLeft = slider.scrollLeft;
    });

    slider.addEventListener('mouseleave', function () {
        isDown = false;
        slider.style.cursor = 'grab';
    });

    slider.addEventListener('mouseup', function () {
        isDown = false;
        slider.style.cursor = 'grab';
    });

    slider.addEventListener('mousemove', function (e) {
        if (!isDown) return;
        e.preventDefault();
        const x = e.pageX - slider.offsetLeft;
        const walk = (x - startX) * 2;
        slider.scrollLeft = scrollLeft - walk;
    });

    // 设置初始状态
    slider.style.cursor = 'grab';
}

// (Spinning globe interaction code removed)

// 登录注册弹窗相关功能
function initAuthModals() {
    const loginModal = document.getElementById('loginModal');
    const registerModal = document.getElementById('registerModal');
    const signInBtn = document.querySelector('.btn-signin');
    const joinBtn = document.querySelector('.btn-join');
    const closeBtns = document.querySelectorAll('.close');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    
    // 点击Log in按钮打开登录弹窗
    if (signInBtn) {
        signInBtn.addEventListener('click', function() {
            showModal('loginModal');
        });
    }
    
    // 点击Join for free按钮打开注册弹窗
    if (joinBtn) {
        joinBtn.addEventListener('click', function() {
            showModal('registerModal');
        });
    }
    
    // 关闭按钮功能
    closeBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const modalId = this.getAttribute('data-modal');
            hideModal(modalId);
        });
    });
    
    // 点击背景关闭弹窗
    [loginModal, registerModal].forEach(modal => {
        if (modal) {
            modal.addEventListener('click', function(e) {
                if (e.target === this) {
                    hideModal(this.id);
                }
            });
        }
    });
    
    // ESC键关闭弹窗
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            hideModal('loginModal');
            hideModal('registerModal');
        }
    });
    
    // 登录表单处理
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            handleLogin(this);
        });
    }
    
    // 注册表单处理
    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault();
            handleRegister(this);
        });
    }
}

// 显示弹窗
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.classList.add('show');
        }, 10);
        
        // 防止背景滚动
        document.body.style.overflow = 'hidden';
        
        // 聚焦到第一个输入框
        const firstInput = modal.querySelector('input');
        if (firstInput) {
            setTimeout(() => firstInput.focus(), 300);
        }
    }
}

// 隐藏弹窗
function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal && modal.classList.contains('show')) {
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
        }, 300);
        
        // 恢复背景滚动
        document.body.style.overflow = '';
        
        // 清空表单
        const form = modal.querySelector('form');
        if (form) {
            form.reset();
            clearFormErrors(form);
        }
    }
}

// 切换到注册弹窗
function switchToRegister() {
    hideModal('loginModal');
    setTimeout(() => {
        showModal('registerModal');
    }, 300);
}

// 切换到登录弹窗
function switchToLogin() {
    hideModal('registerModal');
    setTimeout(() => {
        showModal('loginModal');
    }, 300);
}

// 处理登录
function handleLogin(form) {
    const email = form.email.value;
    const password = form.password.value;
    const remember = form.remember.checked;
    
    // 清除之前的错误
    clearFormErrors(form);
    
    // 验证邮箱
    if (!validateEmail(email)) {
        showFieldError(form.email, '请输入有效的邮箱地址');
        return;
    }
    
    // 验证密码
    if (password.length < 6) {
        showFieldError(form.password, '密码长度至少为6位');
        return;
    }
    
    // 模拟登录过程
    const submitBtn = form.querySelector('.btn-auth');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = '登录中...';
    submitBtn.disabled = true;
    
    setTimeout(() => {
        // 模拟登录成功
        showMessage('登录成功！欢迎回来！', 'success');
        hideModal('loginModal');
        
        // 更新导航栏状态（可以添加用户头像等）
        updateNavAfterLogin(email);
        
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    }, 1500);
}

// 处理注册
function handleRegister(form) {
    const name = form.name.value;
    const email = form.email.value;
    const password = form.password.value;
    const confirmPassword = form.confirmPassword.value;
    const terms = form.terms.checked;
    
    // 清除之前的错误
    clearFormErrors(form);
    
    let hasError = false;
    
    // 验证姓名
    if (name.trim().length < 2) {
        showFieldError(form.name, '姓名至少需要2个字符');
        hasError = true;
    }
    
    // 验证邮箱
    if (!validateEmail(email)) {
        showFieldError(form.email, '请输入有效的邮箱地址');
        hasError = true;
    }
    
    // 验证密码
    if (password.length < 6) {
        showFieldError(form.password, '密码长度至少为6位');
        hasError = true;
    }
    
    // 验证确认密码
    if (password !== confirmPassword) {
        showFieldError(form.confirmPassword, '两次输入的密码不一致');
        hasError = true;
    }
    
    // 验证用户协议
    if (!terms) {
        showMessage('请同意用户协议和隐私政策', 'error');
        hasError = true;
    }
    
    if (hasError) return;
    
    // 模拟注册过程
    const submitBtn = form.querySelector('.btn-auth');
    const originalText = submitBtn.textContent;
    submitBtn.textContent = '注册中...';
    submitBtn.disabled = true;
    
    setTimeout(() => {
        // 模拟注册成功
        showMessage('注册成功！欢迎加入YouthMind！', 'success');
        hideModal('registerModal');
        
        // 更新导航栏状态
        updateNavAfterLogin(email);
        
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
    }, 1500);
}

// 验证邮箱格式
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// 显示字段错误
function showFieldError(field, message) {
    // 移除旧的错误提示
    const existingError = field.parentNode.querySelector('.field-error');
    if (existingError) {
        existingError.remove();
    }
    
    // 添加错误样式
    field.style.borderColor = '#ef4444';
    field.style.backgroundColor = '#fef2f2';
    
    // 创建错误提示
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.textContent = message;
    errorDiv.style.cssText = `
        color: #ef4444;
        font-size: 0.8rem;
        margin-top: 5px;
        animation: fadeIn 0.3s ease;
    `;
    
    field.parentNode.appendChild(errorDiv);
    
    // 聚焦到错误字段
    field.focus();
}

// 清除表单错误
function clearFormErrors(form) {
    // 移除所有错误提示
    const errors = form.querySelectorAll('.field-error');
    errors.forEach(error => error.remove());
    
    // 重置字段样式
    const inputs = form.querySelectorAll('input');
    inputs.forEach(input => {
        input.style.borderColor = '';
        input.style.backgroundColor = '';
    });
}

// 登录后更新导航栏
function updateNavAfterLogin(email) {
    const authButtons = document.querySelector('.auth-buttons');
    if (authButtons) {
        authButtons.innerHTML = `
            <div class="user-info">
                <span class="user-email">${email}</span>
                <button class="btn-logout" onclick="handleLogout()">退出</button>
            </div>
        `;
        
        // 添加用户信息样式
        const style = document.createElement('style');
        style.textContent = `
            .user-info {
                display: flex;
                align-items: center;
                gap: 15px;
            }
            
            .user-email {
                color: var(--text-dark);
                font-weight: 500;
                font-size: 0.9rem;
            }
            
            .btn-logout {
                background: transparent;
                border: 1px solid var(--border-gray);
                color: var(--text-gray);
                padding: 6px 12px;
                border-radius: 15px;
                font-size: 0.8rem;
                cursor: pointer;
                transition: all 0.3s ease;
            }
            
            .btn-logout:hover {
                background-color: var(--light-gray);
                color: var(--text-dark);
            }
        `;
        document.head.appendChild(style);
    }
}

// 处理退出登录
function handleLogout() {
    const authButtons = document.querySelector('.auth-buttons');
    if (authButtons) {
                 authButtons.innerHTML = `
             <button class="btn-signin">Log in</button>
             <button class="btn-join">Join for free</button>
         `;
        
        // 重新绑定事件
        setTimeout(() => {
            initAuthModals();
        }, 100);
        
        showMessage('已成功退出登录', 'success');
    }
} 

// ------------------------------
// 静态世界地图初始化
// ------------------------------
function initWorldMap() {
    const mapElement = document.getElementById('map');
    if (!mapElement || typeof L === 'undefined') return;

    const map = L.map('map', {
        center: [20, 0],
        zoom: 2,
        dragging: false,
        scrollWheelZoom: false,
        doubleClickZoom: false,
        boxZoom: false,
        keyboard: false,
        zoomControl: false,
        tap: false
    });

    // 加载 OpenStreetMap 瓦片
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '',
        interactive: false,
        noWrap: true
    }).addTo(map);

    const markerCoords = [
        [25.2048, 55.2708],   // Dubai
        [51.4545, -2.5879],   // Bristol (UK)
        [39.9042, 116.4074],  // Beijing (China)
        [19.5664, 109.9497]   // Hainan (China)
    ];

    // 添加标记并收集到数组中
    const markers = markerCoords.map(coords => (
        L.circleMarker(coords, {
            radius: 10,
            color: '#ff0000',
            weight: 2,
            fillColor: '#ff3b3b',
            fillOpacity: 1
        }).addTo(map)
    ));

    // 调整视图以包含所有标记
    const bounds = L.latLngBounds(markerCoords);
    map.fitBounds(bounds.pad(0.3));

    // 禁用拖动平移后保持视图固定
    map.once('moveend', () => {
        map.dragging.disable();
    });

} 